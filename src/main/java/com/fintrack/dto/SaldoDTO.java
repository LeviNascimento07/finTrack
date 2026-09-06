package com.fintrack.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * dataInicio/dataFim ficam null quando o saldo e geral (sem filtro de
 * periodo) — presentes apenas quando o GET /saldo informa ambos.
 */
public record SaldoDTO(
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        BigDecimal saldo,
        LocalDate dataInicio,
        LocalDate dataFim
) {
}
