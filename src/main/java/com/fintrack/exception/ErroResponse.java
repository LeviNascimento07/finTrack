package com.fintrack.exception;

import java.time.Instant;

/**
 * Formato unico de corpo de erro da API. Usado pelo AuthenticationEntryPoint,
 * pelo AccessDeniedHandler e pelos exception handlers dos controllers ate a
 * etapa 5 introduzir o GlobalExceptionHandler (que deve manter este formato).
 */
public record ErroResponse(Instant timestamp, int status, String mensagem, String path) {
}
