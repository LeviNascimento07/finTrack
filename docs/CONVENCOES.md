# Convenções de código

Padrões recorrentes a seguir em código novo. Ao ver um padrão se repetir
pela segunda vez, documente aqui — antes disso, é cedo demais para saber se
é convenção ou coincidência.

## Usuário autenticado

Padrão em controllers: parâmetro `@AuthenticationPrincipal UsuarioDetailsImpl`
na assinatura do método — explícito e testável com `@WithMockUser`. Em
services que não recebem esse parâmetro: `UsuarioAutenticado.getId()`
(`com.fintrack.security`), que lê o `SecurityContextHolder` e tem guarda
contra principal anônimo (`"anonymousUser"` é `String`, não
`UsuarioDetailsImpl`; sem guarda dá `ClassCastException`).

## Validação do {usuarioId} da rota

Toda rota no formato `/api/v1/usuarios/{usuarioId}/...` compara o
`usuarioId` do path com `@AuthenticationPrincipal UsuarioDetailsImpl.getId()`
antes de qualquer consulta ao banco (método privado `validarUsuarioDaRota`
repetido em cada controller). Se não bater, lança
`RecursoNaoEncontradoException` (404) — nunca 403, para não confirmar que
aquele id pertence a outra pessoa.

## Tratamento de erro

`GlobalExceptionHandler` (`@RestControllerAdvice`, em
`com.fintrack.exception`) é o ÚNICO lugar que mapeia exceção → status HTTP.
Uma exceção de negócio nova (404/403/409/...) só precisa: (1) a classe da
exceção com uma mensagem de construtor já pronta para o cliente, e (2) uma
entrada `@ExceptionHandler` em `GlobalExceptionHandler` — nunca um handler
local no controller, nunca `@ResponseStatus` na classe da exceção (duas
fontes de verdade sobre o status). Controllers não importam `ErroResponse`
nem têm `construirErro`.

`ErroResponse` (record) é o formato único de corpo de erro:
`timestamp, status, erro, mensagem, path, campos`. `campos` (lista de
`ErroCampoDTO(campo, mensagem)`) é `null`/omitido (`@JsonInclude(NON_NULL)`)
em todo erro que não seja de validação por campo — o formato-base de 5
chaves é sempre o mesmo. Construa toda instância via `ErroResponse.of(...)`,
nunca com `new ErroResponse(...)` direto, para não esquecer `erro`
(`status.getReasonPhrase()`) ou duplicar a montagem do timestamp.

`JwtAuthenticationEntryPoint` (401) e `JwtAccessDeniedHandler` (403) usam
esse mesmo `ErroResponse.of(...)` mas escrevem a resposta manualmente
(`response.getOutputStream()` + `ObjectMapper`, nunca `getWriter()` — ver
armadilha de UTF-8 na etapa 1) porque rodam no filtro de segurança, antes
do `DispatcherServlet`, e por isso uma exceção de autenticação/autorização
nunca chega ao `@RestControllerAdvice`.

O fallback (`Exception.class` → 500) nunca devolve `ex.getMessage()`, nome
de classe ou stack trace no corpo — só loga (`log.error`) no servidor.
Mensagem genérica fixa para o cliente.

## Recurso "não encontrado" nunca distingue de "não seu"

Toda consulta a um recurso pertencente a um usuário usa um método de
repository que já filtra por `usuarioId` (nunca `findById` sozinho — ver a
regra de ouro dos repositories em `CLAUDE.md`). Categoria/transação de
outro usuário e categoria/transação inexistente resultam na mesma exceção
404; a query não distingue os dois casos, e o service não deve tentar
diferenciá-los na resposta.

## Testes

Nomeie toda classe de teste terminando em `Test` (nunca `IT`): o `pom.xml`
só tem o Maven Surefire, sem o Failsafe, então `mvn test` ignora
silenciosamente qualquer classe `*IT.java` — inclusive testes de
integração com `@SpringBootTest`. Testes de contexto Spring usam
`src/test/resources/application.yml` próprio, com um datasource SQLite
dedicado e `ddl-auto: create-drop`; esse arquivo precisa repetir todas as
chaves do `application.yml` principal, não só as que mudam (Spring Boot
carrega um `application.yml` só, o do classpath de teste tem prioridade).

## DTOs

`record` com Bean Validation nas anotações de campo usadas no request. Para
recursos com validações/campos diferentes entre entrada e saída (ex.:
`Transacao`, cujo request valida `@Positive` e a resposta expande dados da
categoria), usar DTOs separados (`XxxRequestDTO`/`XxxResponseDTO`). Para
recursos simples e simétricos (ex.: `Categoria`), um único DTO para request
e response é aceitável.
