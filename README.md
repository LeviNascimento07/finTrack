# FinTrack — Controle de Finanças Pessoais

Sistema desktop de controle de finanças pessoais desenvolvido em **JavaFX**, com persistência em **SQLite**. Permite cadastrar receitas e despesas, visualizá-las em uma tabela e acompanhar o saldo em tempo real.

Projeto desenvolvido para a disciplina de **Java intermediário** (Capacita iRede / Residência TIC-20).

---

## Funcionalidades

- Cadastro de transações (receitas e despesas) por meio de um formulário.
- Listagem de todas as transações em uma tabela, ordenadas da mais recente para a mais antiga.
- Remoção de transações selecionadas.
- Cálculo automático e em tempo real de **total de receitas**, **total de despesas** e **saldo** (receitas − despesas).
- Persistência dos dados em arquivo local (`fintrack.db`) — as transações continuam salvas ao fechar e reabrir o app.

---

## Tecnologias

| Camada        | Tecnologia                     |
|---------------|--------------------------------|
| Linguagem     | Java 21                        |
| Interface     | JavaFX 21 (FXML + CSS)         |
| Persistência  | SQLite (via JDBC)              |
| Build         | Maven                          |
| Testes        | JUnit 5                        |

---

## Arquitetura

O projeto segue uma separação de responsabilidades em camadas, do mais próximo dos dados ao mais próximo da tela:
src/main/java/
├── module-info.java              # Descritor de módulo
└── com/fintrack/
├── app/FinApp.java           # Ponto de entrada (extends Application)
├── model/Transacao.java      # Entidade
├── generic/RepositorioGenerico.java
├── dao/
│   ├── Conexao.java          # Conexão SQLite + criação da tabela
│   └── TransacaoDAO.java     # CRUD com PreparedStatement
├── service/FinanceiroService.java
└── controller/
├── MainController.java    # Tela principal (lista + totais)
└── FormController.java    # Formulário de cadastro
src/main/resources/
├── fxml/
│   ├── main.fxml
│   └── form.fxml
└── css/style.css
src/test/java/com/fintrack/        # Testes JUnit 5
├── model/TransacaoTest.java
├── dao/TransacaoDAOTest.java
└── generic/RepositorioGenericoTest.java

---

## Como executar

### Pré-requisitos

- Java 21 (JDK)
- Maven

### Rodar o aplicativo

Pelo terminal, na raiz do projeto:

```bash
mvn clean javafx:run
```

> **Importante:** não rode pelo botão "Run" (▶) da IDE. O JavaFX não vem embutido no JDK a partir do Java 11, então é necessário rodar via Maven (`javafx:run`), que monta o ambiente do JavaFX corretamente. Rodar pela IDE diretamente resulta no erro *"JavaFX runtime components are missing"*.

No IntelliJ, também é possível rodar pelo painel **Maven -> Plugins -> javafx -> javafx:run** (duplo-clique).

### Rodar os testes

```bash
mvn test
```

São 19 testes no total (model, DAO com banco em memória isolado, e generics).

---

## Banco de dados

O banco SQLite é criado automaticamente na primeira execução (arquivo `fintrack.db` na raiz do projeto). A tabela é criada via `CREATE TABLE IF NOT EXISTS`, então não há passo manual de configuração.

Estrutura da tabela `transacoes`:

| Coluna    | Tipo            | Descrição                       |
|-----------|-----------------|---------------------------------|
| id        | INTEGER (PK)    | Identificador (auto-incremento) |
| descricao | VARCHAR(100)    | Descrição da transação          |
| valor     | DECIMAL(10,2)   | Valor                           |
| tipo      | VARCHAR(10)     | "RECEITA" ou "DESPESA"          |
| data      | DATE            | Data da transação               |

---

## Critérios da disciplina atendidos

| Critério                                             | Onde                                                        |
|------------------------------------------------------|-------------------------------------------------------------|
| **Generics** (`T`, `? extends T`, `? super T`, `?`)  | `RepositorioGenerico.java`                                  |
| **JavaFX** (FXML, Scene Builder, CSS)                | `main.fxml`, `form.fxml`, `style.css`, controllers          |
| **JDBC** (padrão DAO, `PreparedStatement`)           | `Conexao.java`, `TransacaoDAO.java`                         |
| **JUnit** (`@Test`, `@BeforeEach`, `@AfterEach`)     | `TransacaoTest`, `TransacaoDAOTest`, `RepositorioGenericoTest` |

---

## Observação sobre executável standalone

É possível gerar uma imagem standalone com `mvn javafx:jlink`. Porém, a biblioteca `sqlite-jdbc` não é totalmente modular (módulo automático), o que impede o `jlink` de empacotar o driver JDBC corretamente na imagem — resultando em erro *"No suitable driver found"* ao rodar o executável gerado.

Para distribuir o aplicativo como executável, o caminho recomendado é gerar um "fat jar" (com `maven-shade-plugin`) ou um instalador via `jpackage`, que lidam melhor com dependências não modulares. Para execução em ambiente de desenvolvimento, `mvn javafx:run` funciona normalmente.
