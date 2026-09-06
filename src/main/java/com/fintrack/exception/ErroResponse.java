package com.fintrack.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        String path,
        List<ErroCampoDTO> campos
) {

    public static ErroResponse of(HttpStatus status, String mensagem, String path) {
        return new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(), mensagem, path, null);
    }

    public static ErroResponse of(HttpStatus status, String mensagem, String path, List<ErroCampoDTO> campos) {
        return new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(), mensagem, path, campos);
    }
}
