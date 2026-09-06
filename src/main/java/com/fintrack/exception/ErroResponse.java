package com.fintrack.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

/**
 * Formato UNICO de corpo de erro da API inteira: usado pelo
 * GlobalExceptionHandler, pelo AuthenticationEntryPoint (401) e pelo
 * AccessDeniedHandler (403) — os tres pontos de entrada para uma resposta
 * de erro, ja que os dois ultimos rodam no filtro de seguranca, antes do
 * DispatcherServlet, e por isso nao passam pelo @RestControllerAdvice.
 * `campos` so aparece (via @JsonInclude) quando o erro e de validacao por
 * campo; nos demais casos o JSON tem exatamente as mesmas cinco chaves.
 */
@Schema(description = "Formato único de corpo de erro de toda a API")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(
        @Schema(description = "Momento em que o erro ocorreu", example = "2026-01-15T14:32:07.123Z")
        Instant timestamp,

        @Schema(description = "Código de status HTTP", example = "404")
        int status,

        @Schema(description = "Reason phrase HTTP correspondente ao status", example = "Not Found")
        String erro,

        @Schema(description = "Mensagem descritiva do erro, em português", example = "Recurso não encontrado")
        String mensagem,

        @Schema(description = "Caminho da requisição que originou o erro", example = "/api/v1/usuarios/1/categorias")
        String path,

        @Schema(description = "Lista de campos inválidos e suas mensagens — presente APENAS em erros de "
                + "validação (400 de MethodArgumentNotValidException/ConstraintViolationException); "
                + "ausente em todo o resto")
        List<ErroCampoDTO> campos
) {

    public static ErroResponse of(HttpStatus status, String mensagem, String path) {
        return new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(), mensagem, path, null);
    }

    public static ErroResponse of(HttpStatus status, String mensagem, String path, List<ErroCampoDTO> campos) {
        return new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(), mensagem, path, campos);
    }
}
