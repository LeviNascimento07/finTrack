package com.fintrack.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais de login")
public record LoginDTO(
        @Schema(description = "E-mail cadastrado", example = "ana.souza@example.com")
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @Schema(description = "Senha em texto plano", example = "minhaSenha123")
        @NotBlank(message = "Senha é obrigatória")
        String senha
) {
}
