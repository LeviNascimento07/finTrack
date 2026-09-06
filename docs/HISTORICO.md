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

### Limitação conhecida

`SaldoInsuficienteException` só age sobre `DESPESA` (ver "Decisões de
projeto" em `CLAUDE.md`). Editar uma `RECEITA` para um valor menor, ou
excluí-la, pode deixar o saldo negativo sem disparar a regra — a validação
só olha o momento em que uma despesa é criada/editada, não os efeitos de
mexer numa receita depois. **Decisão consciente, não bug**: bloquear esse
caminho impediria o usuário de corrigir um lançamento de receita errado
depois de já ter gasto com base nele. Se isso mudar, é decisão de projeto
e precisa ser discutida antes de qualquer etapa futura mexer nisso.

## Etapa 4 — Saldo e relatórios

- `RelatorioService.calcularSaldo`: sem `dataInicio`/`dataFim`, usa
  `sumValorByUsuarioIdAndTipo` (saldo geral); com ambos, usa
  `sumValorByUsuarioIdAndTipoAndDataBetween` — nenhuma query nova, só
  reaproveitamento dos métodos que já existiam desde a etapa 0.
  `totalizarPorCategoria` reaproveita `totalizarPorCategoria` do
  repository. Todo valor passa por `normalizar` (`setScale(2,
  HALF_UP)`) antes de entrar no DTO — o `coalesce(sum(...), 0)` do
  repository evita `null`, mas não garante escala 2 depois da soma.
- Validação de intervalo isolada em `validarIntervalo`: exatamente uma das
  datas informada, ou `dataInicio` depois de `dataFim`, ambos 400 via
  `IntervaloDataInvalidoException` (nova).
- `RelatorioController` em `/api/v1/usuarios/{usuarioId}/saldo` e
  `/api/v1/usuarios/{usuarioId}/relatorios/por-categoria` (`tipo` default
  `DESPESA`), mesmo padrão de `validarUsuarioDaRota` das etapas anteriores.
- Confirmado em runtime (não só em teste com mock): usuário recém-criado
  sem nenhuma transação recebe `{"totalReceitas":0.00,...}` e `[]` no
  relatório — nunca `null` nem erro. `RelatorioServiceTest` (Mockito)
  cobre esse caso e mais 4: soma geral, soma por período, `dataInicio` >
  `dataFim`, e apenas uma data informada.

### Armadilhas

- Nenhuma nova.

## Etapa 5 — GlobalExceptionHandler

- Levantamento antes de codar: não havia `@ResponseStatus` em nenhuma
  exceção (busca em todo `src/main/java` sem resultado) — o status era
  decidido por `@ExceptionHandler` locais duplicados nos 4 controllers,
  cada um com seu próprio `construirErro`. A API tinha **2 formatos** de
  corpo de erro: o `ErroResponse` customizado (`timestamp, status,
  mensagem, path`) nesses handlers locais e no entry point/access denied
  handler; e o corpo padrão do Spring Boot (`timestamp, status, error,
  path`, sem mensagem — `server.error.include-message` é `never` por
  padrão) sempre que uma exceção não tinha handler local:
  `MethodArgumentNotValidException`, `ConstraintViolationException`,
  `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`,
  e qualquer 500 não tratado. `server.error.include-stacktrace` já não
  estava como `always` (ausente do `application.yml`, padrão `never`).
- `ErroResponse` ganhou os campos `erro` (`status.getReasonPhrase()`) e
  `campos` (lista opcional de `ErroCampoDTO`, omitida via
  `@JsonInclude(NON_NULL)` quando não é erro de validação por campo) — ver
  `docs/CONVENCOES.md` para a convenção completa (`ErroResponse.of(...)`,
  nunca handler local, nunca `@ResponseStatus`).
- `GlobalExceptionHandler` (`@RestControllerAdvice`) concentra todo o
  mapeamento; os 4 controllers perderam seus `@ExceptionHandler` locais e
  o helper `construirErro`. `JwtAuthenticationEntryPoint`/
  `JwtAccessDeniedHandler` passaram a construir o corpo via
  `ErroResponse.of(...)`, unificando de vez com o handler global.
- `GlobalExceptionHandlerTest` (integração): prova que 400 (validação),
  404 (`{usuarioId}` de outra pessoa) e 409 (e-mail duplicado) devolvem as
  mesmas 5 chaves-base no corpo.

### Armadilhas

- Não foi possível disparar organicamente um 500 real via requisições HTTP
  para confirmar o fallback fim-a-fim — todo input malformado testado
  (JSON quebrado, enum inválido no corpo e na query, tipo de path variable
  errado) já caía num handler específico (400/404/409). Fechado depois
  com `GlobalExceptionHandlerFallbackTest` (unitário, chama
  `handleFallback` direto com uma exceção sentinela e afirma que a
  mensagem interna não aparece em nenhum campo do corpo) — não precisa de
  contexto Spring nem de rota real.

## Etapa 6 — Swagger

- `OpenApiConfig` (`com.fintrack.config`): `@OpenAPIDefinition` com `info`
  e `security = @SecurityRequirement(name = "bearerAuth")` global, mais
  `@SecurityScheme(type = HTTP, scheme = "bearer", bearerFormat = "JWT")`
  — só anotações, sem bean `OpenAPI` manual. `AuthController.register`/
  `login` usam `@SecurityRequirements` (vazio) para sobrescrever o global
  e aparecer como público no Swagger UI. Confirmado no `/v3/api-docs`
  gerado: 1 `security` de nível raiz com `bearerAuth` + só 2 overrides
  vazios (register, login) — todo o resto herda o global implicitamente,
  sem precisar repetir a anotação endpoint a endpoint.
- `@Schema(description, example)` em todo campo de todo DTO (records:
  anotação direta no componente do canonical constructor, funciona igual
  a `@NotBlank` etc.). `ErroResponse` também ganhou `@Schema` — inclusive
  no campo `campos`, documentando que só existe em erro de validação.
  Exemplo do JWT em `TokenDTO` é uma string claramente fictícia
  (`EXEMPLO-FICTICIO-NAO-E-UM-TOKEN-REAL`); nenhum DTO de response tem
  campo `senha` (só `RegistroDTO`/`LoginDTO`, que são só request).
- `@Tag` por controller, `@Operation` + `@ApiResponses` (referenciando
  `ErroResponse.class` em todo erro) por endpoint, com ênfase em
  401/403/404/409 conforme pedido.

### Armadilhas

- Nenhuma nova — springdoc 2.6.0 já estava no `pom.xml` desde a etapa 0 e
  as rotas de Swagger já eram públicas no `SecurityConfig` desde a etapa 1.
- Validação do fluxo "Authorize" do Swagger UI feita via curl (login →
  header `Authorization: Bearer`), não num browser real (ambiente
  headless) — o mecanismo testado é o mesmo que o botão aciona, mas não é
  literalmente um clique na UI.

## Etapa 7 — Testes

Foco em fechar lacunas (13 → 34 testes), não inflar número. Cobertura nova:
`CategoriaServiceTest` (7, Mockito: nome duplicado x2, editar/excluir
global, excluir com transações, categoria de outro usuário, listagem
global+própria), `AuthServiceTest` (3, Mockito: e-mail duplicado, senha
errada e e-mail inexistente — mesma exceção/mensagem nos dois),
`CategoriaRepositoryTest`/`TransacaoRepositoryTest` (8, `@DataJpaTest` +
`Replace.NONE`: `findVisiveisPara`, `findAcessivelPor`, as duas somas,
`totalizarPorCategoria`, `findComFiltros` isolados e combinados),
`TokenJwtInvalidoTest` (3, `@SpringBootTest`: token expirado, assinatura
inválida, header sem `Bearer` — os três 401 via `JwtAuthenticationEntryPoint`,
sem depender de nenhum dado persistido).

### Achados reais (conforme pedido, reportados antes de qualquer correção)

- `TransacaoRepository.totalizarPorCategoria` não tinha `ORDER BY` — o
  escopo desta etapa pedia um teste confirmando que o método "agrupa e
  ordena corretamente", mas `GROUP BY` sem `ORDER BY` não garante ordem
  nenhuma (por sorte "funcionava" no SQLite por comportamento não
  especificado). Usuário optou por adicionar `order by c.nome` à query
  (pequena mudança de produção, aprovada antes de escrever o teste) em vez
  de só testar o agrupamento ignorando ordem.
- `@Mock` em `JwtService` (classe concreta) quebrava com
  `MockitoException: Java 24 ... not supported by ... Byte Buddy` neste
  ambiente — não é bug de produção, é limitação de tooling. Resolvido
  usando uma instância real de `JwtService` em `AuthServiceTest` (ver
  `docs/CONVENCOES.md`), já que nenhum dos três cenários testados chega a
  invocá-la.

### Armadilhas

- Nenhuma armadilha nova de SQLite/config — as duas acima já cobertas
  como achados.
