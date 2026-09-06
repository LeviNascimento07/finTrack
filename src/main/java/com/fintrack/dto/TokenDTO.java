package com.fintrack.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token JWT emitido após registro ou login")
public record TokenDTO(
        @Schema(
                description = "Token JWT — envie no header Authorization como 'Bearer {token}' nos demais endpoints",
                example = "eyJhbGciOiJIUzUxMiJ9.EXEMPLO-FICTICIO-NAO-E-UM-TOKEN-REAL.assinatura-de-exemplo"
        )
        String token
) {
}
