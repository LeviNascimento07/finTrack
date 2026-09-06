package com.fintrack.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Total de transações de um tipo (RECEITA ou DESPESA) agrupado por categoria")
public record TotalPorCategoriaDTO(
        @Schema(description = "Nome da categoria", example = "Alimentação")
        String categoriaNome,

        @Schema(description = "Soma dos valores das transações dessa categoria, no tipo consultado", example = "450.00")
        BigDecimal total
) {
}
