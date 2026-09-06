# FinTrack API — briefing operacional

Migração do FinTrack de app desktop JavaFX + JDBC para API RESTful em Spring
Boot. Código desktop preservado na branch `javafx-desktop`.

Além deste arquivo, consulte:
- `docs/CONVENCOES.md` — padrões de código recorrentes (usuário autenticado,
  formato de erro, validação de `{usuarioId}` na rota, DTOs). **Leia antes
  de escrever código novo.**
- `docs/HISTORICO.md` — o que foi implementado em cada etapa concluída e as
  armadilhas já resolvidas. Consulte para contexto do porquê de uma decisão
  passada. Sem teto de tamanho.

## Stack fixa (protegida — ver protocolo abaixo)

- Java 21, Spring Boot 3.3.5 (spring-boot-starter-parent)
- Banco: **SQLite** (decisão definitiva, não sugerir H2/Postgres) via
  `org.xerial:sqlite-jdbc` + `hibernate-community-dialects` (`SQLiteDialect`)
- Segurança: Spring Security + JWT via `io.jsonwebtoken` (jjwt) 0.12.6
- Documentação: springdoc-openapi (Swagger UI em `/swagger-ui.html`)
- DTOs de request/response como `record` (Java 21)

### Restrições do SQLite

- `hikari.maximum-pool-size: 1` — SQLite aceita um único escritor por vez;
  pool maior causa `SQLITE_BUSY`.
- A URL do datasource **precisa** do parâmetro `?foreign_keys=on`. Sem ele o
  SQLite ignora chaves estrangeiras silenciosamente.
- `ddl-auto: update` no Hibernate **não faz ALTER TABLE** no dialeto SQLite.
  Ao mudar uma entidade existente, apague `fintrack.db` e deixe o Hibernate
  recriar o schema do zero.

## Modelo de domínio

- `Usuario`: id, nome, email (unique), senha (hash BCrypt), criadoEm.
- `Categoria`: id, nome, usuario (`@ManyToOne` LAZY, nullable). `usuario ==
  null` → global, visível a todos e imutável. `usuario != null` →
  customizada, pertence só ao dono.
- `Transacao`: id, descricao, valor (`BigDecimal`, precision 12 scale 2 —
  nunca `double`), tipo (`RECEITA`/`DESPESA`, enum STRING), data, usuario e
  categoria (`@ManyToOne` LAZY, `optional=false`). Índice em
  `(usuario_id, data)`. **O valor é sempre positivo**; quem define o sinal
  no saldo é o `tipo`, não o sinal do número.

### Regra de ouro dos repositories

Nenhum método de `TransacaoRepository`/`CategoriaRepository` pode existir
sem `usuarioId` no filtro. O isolamento entre usuários é garantido na
camada de dados, não só na de segurança.

## Decisões de projeto (protegido — ver protocolo abaixo)

- Recurso de outro usuário retorna **404**, nunca 403 (403 confirmaria que
  o id existe para outro dono).
- `SaldoInsuficienteException`: despesa que deixaria o saldo negativo
  retorna **409**, com a verificação isolada em método próprio do service.
  Saldo = soma de receitas menos soma de despesas do usuário, sem recorte
  de data; na edição, desconsidera a contribuição da própria transação
  sendo editada antes de recalcular.
- jjwt 0.12.x tem API diferente da 0.9.x — não copiar exemplos antigos.
- Java 21: usar `record` nos DTOs.
- A senha nunca aparece em nenhum DTO de resposta.

## Roteiro das etapas

- [x] 0. Preparação: limpeza do desktop, base Spring Boot, entidades e
      repositories.
- [x] 1. Segurança + JWT + AuthController.
- [x] 2. Categorias (controller + service + DTOs).
- [x] 3. Transações (controller + service + DTOs).
- [x] 4. Saldo e relatórios.
- [x] 5. GlobalExceptionHandler.
- [x] 6. Swagger.
- [ ] 7. Testes **(atual)**
- [ ] 8. README

## Pontos em aberto

(nenhum no momento)

## Protocolo de manutenção

1. **Início da etapa**: leia este arquivo e `docs/CONVENCOES.md` por
   inteiro; consulte `docs/HISTORICO.md` se precisar de contexto. Código
   atual contradizendo este arquivo → PARE e avise o usuário.
2. **Fim da etapa**: implementado e armadilhas vão para `docs/HISTORICO.md`;
   padrão novo e recorrente vai para `docs/CONVENCOES.md`. Este arquivo só
   muda se mudar decisão de projeto, modelo de domínio, ou etapa atual —
   tudo no mesmo commit da etapa.
3. **Seção protegida**: "Decisões de projeto" e "Stack fixa" não podem ser
   alteradas por você. Ache uma errada/inviável? Objeção em "Pontos em
   aberto", avise no chat, siga a regra original até decisão do usuário.
4. **Teto: ~100 linhas, estável** — briefing operacional, não changelog.
   `docs/HISTORICO.md` não tem teto.
5. **Registre o porquê, não o quê.** O que o código já mostra sozinho não
   precisa estar em nenhum dos três arquivos.
6. Mantenha "Pontos em aberto" com dúvidas e decisões pendentes; consulte-a
   no início de cada etapa.
