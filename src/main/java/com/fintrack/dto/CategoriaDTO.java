package com.fintrack.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Usado tanto para request quanto response. No request, apenas "nome"
 * importa (id e global sao ignorados pelo service). Na resposta, os tres
 * campos sao preenchidos.
 */
@Schema(description = "Categoria de transações — global (do sistema) ou customizada pelo usuário")
public record CategoriaDTO(
        @Schema(description = "Identificador da categoria (ignorado no corpo de criação/edição)", example = "3")
        Long id,

        @Schema(description = "Nome da categoria", example = "Alimentação")
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @Schema(description = "true = categoria global do sistema (imutável); false = customizada pelo usuário (ignorado no request)", example = "false")
        boolean global
) {
}
