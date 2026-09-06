package com.fintrack.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para registro de um novo usuário")
public record RegistroDTO(
        @Schema(description = "Nome completo do usuário", example = "Ana Souza")
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @Schema(description = "E-mail único, usado para login", example = "ana.souza@example.com")
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @Schema(description = "Senha em texto plano — nunca armazenada nem devolvida; é hasheada com BCrypt", example = "minhaSenha123")
        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        String senha
) {
}
