package com.fintrack.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Usado tanto para request quanto response. No request, apenas "nome"
 * importa (id e global sao ignorados pelo service). Na resposta, os tres
 * campos sao preenchidos.
 */
public record CategoriaDTO(
        Long id,

        @NotBlank(message = "Nome é obrigatório")
        String nome,

        boolean global
) {
}
