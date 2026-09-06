# Histórico de implementação

Registro do que foi feito em cada etapa concluída e das armadilhas
encontradas pelo caminho. Sem teto de tamanho — é apêndice, não briefing.
Padrões nascidos desse histórico e que valem para código novo vivem em
`docs/CONVENCOES.md`. Decisões de projeto e stack ficam só em `CLAUDE.md`.

## Etapa 1 — Segurança + JWT + AuthController

- `SecurityConfig` STATELESS/CSRF off (público: `/api/auth/**`,
  `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`, `/error`) +
  `AuthenticationEntryPoint` (401) + `AccessDeniedHandler` (403).
- `JwtService`/`JwtProperties` (jjwt 0.12.6) + `JwtAuthenticationFilter` +
  `UsuarioDetailsService`/`UsuarioDetailsImpl` (principal com `getId()`).
- `AuthController` (`register` 201 / `login` 200) com
  `EmailJaCadastradoException` (409) e `CredenciaisInvalidasException` (401
  genérico).
- `ErroResponse` (`com.fintrack.exception`) criado aqui — formato e uso
  documentados em `docs/CONVENCOES.md`.

### Armadilhas

- `/error` fora do `permitAll()` fazia falha de `@Valid` virar 401 em vez de
  400 (o forward interno do Spring para `/error` passa pela cadeia de
  segurança).
- `response.getWriter()` no entry point/access denied handler usa
  ISO-8859-1 por padrão — corrigido com `setCharacterEncoding("UTF-8")` +
  `getOutputStream()`.
- `/swagger-ui.html` não é coberto por `/swagger-ui/**` (rota de redirect
  própria) — precisou de entrada explícita em `permitAll()`.
- `mvn spring-boot:run` deu `ClassNotFoundException` neste ambiente (Git
  Bash/Windows); validação em runtime sempre via `mvn clean package` +
  `java -jar` usando `"$JAVA_HOME/bin/java"` (o `java` do PATH é um JDK 8
  via SDKMAN, incompatível com bytecode Java 21).

## Etapa 2 — Categorias

- `DataInitializer` (`CommandLineRunner`, `com.fintrack.config`): semeia as
  8 categorias globais, checando `existsByNomeIgnoreCaseAndUsuarioIsNull`
  antes de cada insert — idempotente (testado reiniciando o app duas vezes,
  sem duplicar).
- `CategoriaService`: `listar` usa `findVisiveisPara`; criar/atualizar/excluir
  passam por `buscarPropriaEditavel` (usa `findAcessivelPor`, que só retorna
  global ou própria — id de outro usuário nunca aparece, vira 404
  automaticamente) + checagem `isGlobal()` → 403.
- `CategoriaController` em `/api/v1/usuarios/{usuarioId}/categorias`.
  Exceções novas: `CategoriaNaoEncontradaException` (404),
  `CategoriaGlobalImutavelException` (403),
  `NomeCategoriaDuplicadoException`/`CategoriaComTransacoesException` (409).

### Armadilhas

- Nenhuma nova: reaproveitou os handlers e o formato de erro da etapa 1
  sem ajustes.

## Etapa 3 — Transações

- `TransacaoRepository.findComFiltros`: JPQL com `categoriaId`/`inicio`/`fim`
  opcionais (`:param is null or ...`), paginado; a ordenação vem do
  `Pageable` (não da query), pra não colidir com o `order by` do Spring
  Data.
- `TransacaoService.validarSaldoSuficiente(usuarioId, novoTipo, novoValor,
  transacaoAtual)`: método isolado, só age se `novoTipo == DESPESA`. Na
  edição, remove a contribuição antiga da própria transação (`+valor` se
  era RECEITA, `-valor` se era DESPESA) do saldo antes de aplicar o novo
  valor — cobre inclusive o caso de trocar o tipo da transação no mesmo PUT.
  Testado com Mockito (`TransacaoServiceTest`): saldo suficiente, saldo
  insuficiente na criação, e edição de despesa de 100→101 com headroom
  (cenário que uma implementação ingênua rejeitaria por contar o valor
  antigo duas vezes).
- `TransacaoController` em `/api/v1/usuarios/{usuarioId}/transacoes`:
  categoria do request validada via `findAcessivelPor` (categoria de outro
  usuário → 404, reaproveitando `CategoriaNaoEncontradaException`);
  transação buscada sempre via `findByIdAndUsuarioId` (nunca `findById`).
  `TransacaoNaoEncontradaException` (404) e `SaldoInsuficienteException`
  (409) novas.
- `TransacaoAcessoCruzadoTest` (integração, `@SpringBootTest` +
  `@AutoConfigureMockMvc`): usuário A com seu próprio `{usuarioId}` correto
  na rota, mas tentando GET/PUT/DELETE numa transação do usuário B — 404
  nos três casos, provando o isolamento de `findByIdAndUsuarioId`.

### Armadilhas

- `mvn test` (Surefire puro, sem Failsafe configurado no `pom.xml`) só roda
  classes terminadas em `Test`/`Tests`/`TestCase` — uma classe `*IT.java`
  (convenção do Failsafe) é silenciosamente ignorada, sem erro nem aviso.
  O teste de integração desta etapa foi renomeado de `...IT` para `...Test`
  depois de rodar `mvn test` e notar que só os testes unitários apareciam
  no relatório.
- Testes usam `src/test/resources/application.yml` próprio (datasource
  SQLite em memória com cache compartilhado, `ddl-auto: create-drop`) —
  como o Spring Boot carrega só um `application.yml` por execução, esse
  arquivo de teste duplica todas as chaves do principal (não apenas as que
  mudam), senão propriedades como `fintrack.jwt.secret` ficam ausentes no
  contexto de teste.
