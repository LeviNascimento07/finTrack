# FinTrack API — briefing operacional

Migração do FinTrack de app desktop JavaFX + JDBC para API RESTful em Spring
Boot. O código desktop original está preservado na branch `javafx-desktop`.

## Stack fixa (protegida — ver protocolo abaixo)

- Java 21, Spring Boot 3.3.5 (spring-boot-starter-parent)
- Banco: **SQLite** (decisão definitiva, não sugerir H2/Postgres) via
  `org.xerial:sqlite-jdbc` + `hibernate-community-dialects`
  (`SQLiteDialect`)
- Segurança: Spring Security + JWT via `io.jsonwebtoken` (jjwt) 0.12.6
- Documentação: springdoc-openapi (Swagger UI em `/swagger-ui.html`)
- DTOs de request/response como `record` (Java 21)

### Restrições do SQLite

- `hikari.maximum-pool-size: 1` — SQLite aceita um único escritor por vez;
  pool maior causa `SQLITE_BUSY`.
- A URL do datasource **precisa** do parâmetro `?foreign_keys=on`
  (`jdbc:sqlite:fintrack.db?foreign_keys=on`). Sem ele o SQLite ignora
  chaves estrangeiras silenciosamente.
- `ddl-auto: update` no Hibernate **não faz ALTER TABLE** no dialeto SQLite.
  Ao mudar uma entidade existente, apague `fintrack.db` e deixe o Hibernate
  recriar o schema do zero.

## Modelo de domínio

- `Usuario`: id, nome, email (unique), senha (hash BCrypt), criadoEm.
- `Categoria`: id, nome, usuario (`@ManyToOne` LAZY, nullable).
  `usuario == null` → categoria global do sistema, visível a todos e
  imutável. `usuario != null` → customizada, pertence só ao dono.
- `Transacao`: id, descricao, valor (`BigDecimal`, precision 12 scale 2 —
  nunca `double`), tipo (`RECEITA`/`DESPESA`, enum STRING), data, usuario
  e categoria (`@ManyToOne` LAZY, `optional=false`). Índice em
  `(usuario_id, data)`. **O valor é sempre positivo**; quem define o sinal
  no saldo é o `tipo`, não o sinal do número.

### Regra de ouro dos repositories

Nenhum método de `TransacaoRepository`/`CategoriaRepository` pode existir
sem `usuarioId` no filtro. O isolamento entre usuários é garantido na
camada de dados, não só na de segurança — um bug de autorização no service
não pode vazar dado de outro usuário se o repository já filtra por dono.

## Decisões de projeto (protegido — ver protocolo abaixo)

- Recurso de outro usuário retorna **404**, nunca 403 (403 confirmaria que
  o id existe para outro dono).
- `SaldoInsuficienteException`: despesa que deixaria o saldo negativo
  retorna **409**, com a verificação isolada em método próprio do service
  (não misturada com a lógica de persistência).
- jjwt 0.12.x tem API diferente da 0.9.x (não usar `Jwts.parser()` no
  formato antigo nem `SignatureAlgorithm` depreciado) — não copiar exemplos
  antigos da lib.
- Java 21: usar `record` nos DTOs.
- A senha nunca aparece em nenhum DTO de resposta.

## Roteiro das etapas

- [x] 0. Preparação: limpeza do desktop, base Spring Boot, entidades e
      repositories.
- [x] 1. Segurança + JWT + AuthController.
- [ ] 2. Categorias (controller + service + DTOs) **(próxima etapa)**
- [ ] 3. Transações (controller + service + DTOs)
- [ ] 4. Saldo e relatórios
- [ ] 5. GlobalExceptionHandler
- [ ] 6. Swagger
- [ ] 7. Testes
- [ ] 8. README

### Etapa 1 — o que foi implementado

- `SecurityConfig`: STATELESS, CSRF off, público `/api/auth/**`,
  `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`, `/error`; resto
  autenticado. Beans `PasswordEncoder` (BCrypt), `DaoAuthenticationProvider`,
  `AuthenticationManager`, `AuthenticationEntryPoint` (401) e
  `AccessDeniedHandler` (403).
- `JwtService`/`JwtProperties` (jjwt 0.12.6, `Keys.hmacShaKeyFor` +
  `verifyWith`, sem API antiga) + `JwtAuthenticationFilter` +
  `UsuarioDetailsService`/`UsuarioDetailsImpl` (principal com `getId()`).
- `AuthController`: `POST /api/auth/register` (201) e `/login` (200);
  `RegistroDTO`/`LoginDTO`/`TokenDTO` (records + Bean Validation);
  `EmailJaCadastradoException` (409) e `CredenciaisInvalidasException` (401
  genérico) tratadas por `@ExceptionHandler` local — sem
  `GlobalExceptionHandler` ainda (etapa 5).
- `ErroResponse` (`com.fintrack.exception`): formato único de erro
  (`timestamp, status, mensagem, path`) usado por entry point, access denied
  handler e handlers locais — a etapa 5 deve reusá-lo, não inventar outro.

### Decisão: usuário autenticado nas etapas 2 e 3

Padrão em controllers: `@AuthenticationPrincipal UsuarioDetailsImpl` na
assinatura (explícito, testável com `@WithMockUser`). Em services sem esse
parâmetro: `UsuarioAutenticado.getId()` (`com.fintrack.security`) — tem
guarda contra principal anônimo (`"anonymousUser"` é `String`, não
`UsuarioDetailsImpl`; sem guarda dá `ClassCastException`).

### Armadilhas encontradas

- `/error` fora do `permitAll()` fazia falha de `@Valid` virar 401 em vez de
  400 (o forward interno do Spring para `/error` passa pela cadeia de
  segurança).
- `response.getWriter()` no entry point/access denied handler usa
  ISO-8859-1 por padrão — corrigido com `setCharacterEncoding("UTF-8")` +
  `getOutputStream()`.
- `/swagger-ui.html` não é coberto por `/swagger-ui/**` (rota de redirect
  própria) — precisa entrada explícita em `permitAll()`.
- `mvn spring-boot:run` deu `ClassNotFoundException` neste ambiente (Git
  Bash/Windows); validado com `mvn clean package` + `java -jar` usando
  `"$JAVA_HOME/bin/java"` (o `java` do PATH era um JDK 8 via SDKMAN).

## Pontos em aberto

(nenhum no momento)

## Protocolo de manutenção

1. **No início de cada etapa**: leia este arquivo inteiro antes de escrever
   código. Se algo aqui contradisser o código atual do repositório, PARE e
   avise o usuário — não decida sozinho quem está certo.
2. **Ao final de cada etapa**, atualize este arquivo no mesmo commit da
   etapa, com: o que foi implementado (uma linha por item, não um
   relatório), decisões técnicas novas que afetem etapas futuras,
   armadilhas encontradas e como foram resolvidas, e a próxima etapa
   marcada como atual.
3. **Seção protegida**: "Decisões de projeto" e "Stack fixa" não podem ser
   alteradas ou removidas por você. Se concluir que uma delas está errada
   ou inviável, escreva a objeção em "Pontos em aberto", avise no chat e
   siga a regra original até o usuário decidir.
4. **Teto de tamanho**: ~150 linhas. Ao se aproximar disso, condense o
   histórico em vez de acrescentar — detalhe de implementação já concluída
   e estável pode virar uma linha ou sair. Este arquivo é briefing
   operacional, não changelog.
5. **Registre o porquê, não o quê.** "Usa X" não ajuda ninguém; "usa X
   porque Y quebrou com SQLite" evita que a próxima etapa refaça o erro. O
   que o código já mostra sozinho não precisa estar aqui.
6. Mantenha a seção "Pontos em aberto" com dúvidas e decisões pendentes.
   Consulte-a no início de cada etapa.
