package com.fintrack.service;

import com.fintrack.dto.SaldoDTO;
import com.fintrack.dto.TotalPorCategoriaDTO;
import com.fintrack.exception.IntervaloDataInvalidoException;
import com.fintrack.model.TipoTransacao;
import com.fintrack.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class RelatorioService {

    private final TransacaoRepository transacaoRepository;

    public RelatorioService(TransacaoRepository transacaoRepository) {
        this.transacaoRepository = transacaoRepository;
    }

    /**
     * Sem dataInicio/dataFim: saldo geral, via sumValorByUsuarioIdAndTipo
     * (o coalesce ali garante zero, nunca null, para usuario sem transacoes).
     * Com ambos: recorte de periodo via sumValorByUsuarioIdAndTipoAndDataBetween.
     */
    @Transactional(readOnly = true)
    public SaldoDTO calcularSaldo(Long usuarioId, LocalDate dataInicio, LocalDate dataFim) {
        validarIntervalo(dataInicio, dataFim);

        BigDecimal receitas;
        BigDecimal despesas;
        if (dataInicio != null) {
            receitas = transacaoRepository.sumValorByUsuarioIdAndTipoAndDataBetween(
                    usuarioId, TipoTransacao.RECEITA, dataInicio, dataFim);
            despesas = transacaoRepository.sumValorByUsuarioIdAndTipoAndDataBetween(
                    usuarioId, TipoTransacao.DESPESA, dataInicio, dataFim);
        } else {
            receitas = transacaoRepository.sumValorByUsuarioIdAndTipo(usuarioId, TipoTransacao.RECEITA);
            despesas = transacaoRepository.sumValorByUsuarioIdAndTipo(usuarioId, TipoTransacao.DESPESA);
        }

        receitas = normalizar(receitas);
        despesas = normalizar(despesas);
        BigDecimal saldo = normalizar(receitas.subtract(despesas));

        return new SaldoDTO(receitas, despesas, saldo, dataInicio, dataFim);
    }

    @Transactional(readOnly = true)
    public List<TotalPorCategoriaDTO> totalizarPorCategoria(Long usuarioId, TipoTransacao tipo) {
        return transacaoRepository.totalizarPorCategoria(usuarioId, tipo).stream()
                .map(total -> new TotalPorCategoriaDTO(total.getNomeCategoria(), normalizar(total.getTotal())))
                .toList();
    }

    private void validarIntervalo(LocalDate dataInicio, LocalDate dataFim) {
        boolean apenasUmInformado = (dataInicio == null) != (dataFim == null);
        if (apenasUmInformado) {
            throw new IntervaloDataInvalidoException("Informe dataInicio e dataFim juntos, ou nenhum dos dois");
        }
        if (dataInicio != null && dataInicio.isAfter(dataFim)) {
            throw new IntervaloDataInvalidoException("dataInicio não pode ser posterior a dataFim");
        }
    }

    private BigDecimal normalizar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
