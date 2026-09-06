package com.fintrack.dto;

import com.fintrack.model.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransacaoResponseDTO(
        Long id,
        String descricao,
        BigDecimal valor,
        TipoTransacao tipo,
        LocalDate data,
        Long categoriaId,
        String categoriaNome
) {
}
