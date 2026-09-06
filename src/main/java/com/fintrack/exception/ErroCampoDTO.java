package com.fintrack.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Um campo inválido e a mensagem de validação correspondente")
public record ErroCampoDTO(
        @Schema(description = "Nome do campo do request que falhou na validação", example = "email")
        String campo,

        @Schema(description = "Mensagem de validação específica desse campo", example = "E-mail inválido")
        String mensagem
) {
}
