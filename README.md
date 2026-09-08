# FinTrack API

API RESTful de controle de finanças pessoais: cadastro de usuários, categorias
(globais e customizadas), transações (receitas e despesas) com validação de
saldo, e relatórios de saldo/distribuição de gastos por categoria.

> **Nota de versão:** este é o FinTrack migrado para API REST em Spring Boot.
> A versão original, um app desktop em JavaFX + JDBC, está preservada
> integralmente na branch `javafx-desktop`.

## Stack

| Camada          | Tecnologia                                             |
|-----------------|---------------------------------------------------------|
| Linguagem       | Java 21                                                  |
| Framework       | Spring Boot 3.3.5 (Web, Validation, Data JPA, Security)  |
| Banco           | SQLite (`org.xerial:sqlite-jdbc` + `hibernate-community-dialects`) |
| Autenticação    | Spring Security + JWT (`io.jsonwebtoken` / jjwt 0.12.6)  |
| Documentação    | springdoc-openapi (Swagger UI)                           |
| DTOs            | `record` (Java 21)                                       |
| Testes          | JUnit 5 + Mockito + `@SpringBootTest` / `@DataJpaTest`   |
| Build           | Maven                                                    |

## Pré-requisitos

- JDK 21
- Maven

## Como rodar

Pelo terminal, na raiz do projeto:

```bash
mvn spring-boot:run
```

Ou gerando o jar e executando diretamente:

```bash
mvn clean package
java -jar target/fintrack-api-1.0.0.jar
```

A aplicação sobe em `http://localhost:8080`. Na primeira execução, o SQLite
cria o arquivo `fintrack.db` na raiz do projeto e o Hibernate cria o schema;
um `CommandLineRunner` semeia 8 categorias globais (Alimentação, Transporte,
Moradia, Saúde, Educação, Lazer, Salário, Outros), de forma idempotente.

## Como autenticar

Os únicos endpoints públicos são `/api/auth/register` e `/api/auth/login`;
todo o resto exige um token JWT no header `Authorization: Bearer {token}`.
Fluxo completo (registrar → logar → usar o token):

```bash
# 1. Registrar (já devolve um token, pronto para uso)
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"nome":"Ana Souza","email":"ana.souza@example.com","senha":"minhaSenha123"}'

# 2. Logar (alternativa ao token do registro, ou para uma sessão futura)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana.souza@example.com","senha":"minhaSenha123"}'

# 3. Usar o token nas rotas protegidas (troque {token} pelo valor recebido acima
#    e 1 pelo id do usuário autenticado, presente em toda rota /usuarios/{usuarioId}/...)
TOKEN="{token}"

curl -X GET http://localhost:8080/api/v1/usuarios/1/categorias \
  -H "Authorization: Bearer $TOKEN"
```

Exemplo de fluxo de negócio completo — criar uma receita e uma despesa,
depois consultar saldo e relatório:

```bash
# Receita (categoria 7 = Salário, global)
curl -X POST http://localhost:8080/api/v1/usuarios/1/transacoes \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"descricao":"Salario","valor":4000.00,"tipo":"RECEITA","data":"2026-01-05","categoriaId":7}'

# Despesa (categoria 1 = Alimentação, global)
curl -X POST http://localhost:8080/api/v1/usuarios/1/transacoes \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"descricao":"Supermercado do mes","valor":150.00,"tipo":"DESPESA","data":"2026-01-15","categoriaId":1}'

# Saldo geral
curl -X GET http://localhost:8080/api/v1/usuarios/1/saldo \
  -H "Authorization: Bearer $TOKEN"
# -> {"totalReceitas":4000.00,"totalDespesas":150.00,"saldo":3850.00,"dataInicio":null,"dataFim":null}

# Distribuição de despesas por categoria
curl -X GET "http://localhost:8080/api/v1/usuarios/1/relatorios/por-categoria?tipo=DESPESA" \
  -H "Authorization: Bearer $TOKEN"
# -> [{"categoriaNome":"Alimentação","total":150.00}]
```

Todos os exemplos acima foram testados contra uma instância local rodando.

## Swagger

Com a aplicação rodando, a documentação interativa fica em
`http://localhost:8080/swagger-ui.html`.

Para chamar rotas protegidas pela UI: faça `login` (ou `register`) num dos
dois endpoints públicos, copie o valor de `token` da resposta, clique no
botão **Authorize** (canto superior direito) e cole o token no campo —
**sem** o prefixo `Bearer `, o Swagger UI já adiciona. Depois disso, todo
endpoint chamado pela UI carrega o header `Authorization` automaticamente.

## Endpoints

Toda rota de recurso segue o padrão `/api/v1/usuarios/{usuarioId}/...`, onde
`{usuarioId}` precisa ser o id do usuário dono do token — outro valor
retorna 404 (nunca 403, para não confirmar que aquele id existe).

| Método | Rota                                                          | Descrição                                             |
|--------|----------------------------------------------------------------|--------------------------------------------------------|
| POST   | `/api/auth/register`                                            | Cria usuário e devolve token JWT                        |
| POST   | `/api/auth/login`                                                | Autentica e devolve token JWT                            |
| GET    | `/api/v1/usuarios/{usuarioId}/categorias`                       | Lista categorias globais + próprias do usuário           |
| POST   | `/api/v1/usuarios/{usuarioId}/categorias`                       | Cria categoria customizada                                |
| PUT    | `/api/v1/usuarios/{usuarioId}/categorias/{id}`                  | Atualiza categoria própria (global é imutável)            |
| DELETE | `/api/v1/usuarios/{usuarioId}/categorias/{id}`                  | Exclui categoria própria sem transações associadas        |
| GET    | `/api/v1/usuarios/{usuarioId}/transacoes`                       | Lista transações, paginado, com filtro opcional de categoria/período |
| GET    | `/api/v1/usuarios/{usuarioId}/transacoes/{id}`                  | Busca uma transação                                       |
| POST   | `/api/v1/usuarios/{usuarioId}/transacoes`                       | Cria transação (valida saldo se for despesa)               |
| PUT    | `/api/v1/usuarios/{usuarioId}/transacoes/{id}`                  | Atualiza transação (revalida saldo se for despesa)          |
| DELETE | `/api/v1/usuarios/{usuarioId}/transacoes/{id}`                  | Exclui transação                                          |
| GET    | `/api/v1/usuarios/{usuarioId}/saldo`                             | Saldo geral, ou de um período com `dataInicio`/`dataFim`   |
| GET    | `/api/v1/usuarios/{usuarioId}/relatorios/por-categoria`         | Soma por categoria, filtrando por `tipo` (default `DESPESA`) |

## Regras de negócio

- **Categorias globais vs. customizadas:** uma categoria sem dono
  (`usuario == null`) é global — visível a todos os usuários e imutável
  (editar/excluir retorna 403). Uma categoria com dono pertence só a ele;
  tentar acessar a categoria de outro usuário retorna 404, igual a uma
  categoria inexistente.
- **Saldo insuficiente:** uma despesa que deixaria o saldo negativo é
  rejeitada com 409. O saldo é a soma de todas as receitas menos a soma de
  todas as despesas do usuário, sem recorte de data. Na edição de uma
  despesa existente, a contribuição atual dela é desconsiderada antes de
  recalcular — editar uma despesa de 100 para 101 não é rejeitado só porque
  o valor antigo (100) seria contado duas vezes.
- **Limitação conhecida:** a validação de saldo só age no momento em que uma
  *despesa* é criada ou editada. Editar uma receita para um valor menor, ou
  excluí-la, pode deixar o saldo negativo sem disparar a regra — decisão
  consciente (ver `docs/HISTORICO.md`, Etapa 3): bloquear esse caminho
  impediria corrigir um lançamento de receita errado depois de já ter
  gasto com base nele.

## Erros

Toda a API devolve o mesmo formato de corpo de erro:

```json
{
  "timestamp": "2026-01-15T14:32:07.123Z",
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Recurso não encontrado",
  "path": "/api/v1/usuarios/1/categorias/99",
  "campos": null
}
```

`campos` só aparece (lista de `{campo, mensagem}`) em erro de validação por
campo (400 de corpo inválido); nos demais casos fica ausente.

| Status | Quando                                                                |
|--------|-------------------------------------------------------------------------|
| 400    | Corpo/parâmetro inválido, malformado ou ausente                        |
| 401    | Token ausente, inválido, expirado, ou credenciais de login incorretas  |
| 403    | Tentativa de alterar/excluir uma categoria global                      |
| 404    | `{usuarioId}` da rota não é o do token, ou recurso inexistente/de outro usuário |
| 409    | E-mail já cadastrado, nome de categoria duplicado, categoria com transações associadas, ou despesa que deixaria o saldo negativo |
| 500    | Erro não tratado — corpo genérico, nunca expõe mensagem interna, stack trace ou nome de classe |

## Testes

```bash
mvn test
```

São 34 testes: testes de unidade dos services com Mockito
(`CategoriaServiceTest`, `TransacaoServiceTest`, `RelatorioServiceTest`,
`AuthServiceTest`), testes de repository com `@DataJpaTest`
(`CategoriaRepositoryTest`, `TransacaoRepositoryTest` — queries JPQL
customizadas, incluindo agrupamento/ordenação e filtros combinados) e testes
de integração com `@SpringBootTest` (`GlobalExceptionHandlerTest`,
`TransacaoAcessoCruzadoTest` — prova que um usuário não acessa transação de
outro mesmo com seu próprio `{usuarioId}` correto na rota — e
`TokenJwtInvalidoTest` — token expirado, assinatura inválida, header sem
`Bearer`).

## Restrições do SQLite

- `hikari.maximum-pool-size: 1` — SQLite aceita um único escritor por vez.
- A URL do datasource precisa do parâmetro `?foreign_keys=on`, senão chaves
  estrangeiras são ignoradas silenciosamente.
- `ddl-auto: update` do Hibernate **não faz `ALTER TABLE`** neste dialeto.
  Ao mudar uma entidade existente, apague `fintrack.db` e deixe o Hibernate
  recriar o schema do zero na próxima subida.
