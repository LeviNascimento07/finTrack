package com.fintrack.dto;

import com.fintrack.model.TipoTransacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Dados para criar ou atualizar uma transação")
public record TransacaoRequestDTO(
        @Schema(description = "Descrição livre da transação", example = "Supermercado do mês")
        @NotBlank(message = "Descrição é obrigatória")
        String descricao,

        @Schema(description = "Valor da transação — sempre positivo; o sinal no saldo vem do campo tipo, não do número", example = "150.00")
        @NotNull(message = "Valor é obrigatório")
        @Positive(message = "Valor deve ser positivo")
        BigDecimal valor,

        @Schema(description = "RECEITA soma no saldo, DESPESA subtrai", example = "DESPESA")
        @NotNull(message = "Tipo é obrigatório")
        TipoTransacao tipo,

        @Schema(description = "Data em que a transação ocorreu", example = "2026-01-15")
        @NotNull(message = "Data é obrigatória")
        LocalDate data,

        @Schema(description = "Id de uma categoria acessível ao usuário (global ou própria)", example = "1")
        @NotNull(message = "Categoria é obrigatória")
        Long categoriaId
) {
}
