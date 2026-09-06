package com.fintrack.dto;

import com.fintrack.model.TipoTransacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Transação já persistida, com o nome da categoria expandido")
public record TransacaoResponseDTO(
        @Schema(description = "Identificador da transação", example = "42")
        Long id,

        @Schema(description = "Descrição livre da transação", example = "Supermercado do mês")
        String descricao,

        @Schema(description = "Valor da transação, sempre positivo", example = "150.00")
        BigDecimal valor,

        @Schema(description = "RECEITA soma no saldo, DESPESA subtrai", example = "DESPESA")
        TipoTransacao tipo,

        @Schema(description = "Data em que a transação ocorreu", example = "2026-01-15")
        LocalDate data,

        @Schema(description = "Id da categoria associada", example = "1")
        Long categoriaId,

        @Schema(description = "Nome da categoria associada, para exibição sem consulta extra", example = "Alimentação")
        String categoriaNome
) {
}
