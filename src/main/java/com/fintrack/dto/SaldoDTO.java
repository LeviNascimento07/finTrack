package com.fintrack.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * dataInicio/dataFim ficam null quando o saldo e geral (sem filtro de
 * periodo) — presentes apenas quando o GET /saldo informa ambos.
 */
@Schema(description = "Saldo geral ou de um período — soma de receitas menos soma de despesas")
public record SaldoDTO(
        @Schema(description = "Soma de todas as transações do tipo RECEITA no escopo consultado", example = "4000.00")
        BigDecimal totalReceitas,

        @Schema(description = "Soma de todas as transações do tipo DESPESA no escopo consultado", example = "550.00")
        BigDecimal totalDespesas,

        @Schema(description = "totalReceitas - totalDespesas", example = "3450.00")
        BigDecimal saldo,

        @Schema(description = "Início do período consultado; null quando o saldo é geral (sem filtro de data)", example = "2026-01-01")
        LocalDate dataInicio,

        @Schema(description = "Fim do período consultado; null quando o saldo é geral (sem filtro de data)", example = "2026-01-31")
        LocalDate dataFim
) {
}
