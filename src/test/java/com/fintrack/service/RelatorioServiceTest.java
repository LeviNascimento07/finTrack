package com.fintrack.service;

import com.fintrack.dto.SaldoDTO;
import com.fintrack.exception.IntervaloDataInvalidoException;
import com.fintrack.model.TipoTransacao;
import com.fintrack.repository.TransacaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    private static final Long USUARIO_ID = 1L;

    @Mock
    private TransacaoRepository transacaoRepository;

    @InjectMocks
    private RelatorioService relatorioService;

    @Test
    void calcularSaldo_usuarioSemTransacoes_retornaZerosNuncaNull() {
        // simula o coalesce(sum(...), 0) do repository: usuario sem
        // lancamentos nunca deve propagar null ate o DTO.
        when(transacaoRepository.sumValorByUsuarioIdAndTipo(USUARIO_ID, TipoTransacao.RECEITA))
                .thenReturn(BigDecimal.ZERO);
        when(transacaoRepository.sumValorByUsuarioIdAndTipo(USUARIO_ID, TipoTransacao.DESPESA))
                .thenReturn(BigDecimal.ZERO);

        SaldoDTO saldo = relatorioService.calcularSaldo(USUARIO_ID, null, null);

        assertThat(saldo.totalReceitas()).isEqualByComparingTo("0.00");
        assertThat(saldo.totalDespesas()).isEqualByComparingTo("0.00");
        assertThat(saldo.saldo()).isEqualByComparingTo("0.00");
        assertThat(saldo.dataInicio()).isNull();
        assertThat(saldo.dataFim()).isNull();
    }

    @Test
    void calcularSaldo_semFiltro_usaSomaGeralESubtrai() {
        when(transacaoRepository.sumValorByUsuarioIdAndTipo(USUARIO_ID, TipoTransacao.RECEITA))
                .thenReturn(new BigDecimal("1000"));
        when(transacaoRepository.sumValorByUsuarioIdAndTipo(USUARIO_ID, TipoTransacao.DESPESA))
                .thenReturn(new BigDecimal("300"));

        SaldoDTO saldo = relatorioService.calcularSaldo(USUARIO_ID, null, null);

        assertThat(saldo.totalReceitas()).isEqualByComparingTo("1000.00");
        assertThat(saldo.totalDespesas()).isEqualByComparingTo("300.00");
        assertThat(saldo.saldo()).isEqualByComparingTo("700.00");
    }

    @Test
    void calcularSaldo_comPeriodo_usaSomaPorIntervalo() {
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fim = LocalDate.of(2026, 1, 31);

        when(transacaoRepository.sumValorByUsuarioIdAndTipoAndDataBetween(USUARIO_ID, TipoTransacao.RECEITA, inicio, fim))
                .thenReturn(new BigDecimal("500"));
        when(transacaoRepository.sumValorByUsuarioIdAndTipoAndDataBetween(USUARIO_ID, TipoTransacao.DESPESA, inicio, fim))
                .thenReturn(new BigDecimal("200"));

        SaldoDTO saldo = relatorioService.calcularSaldo(USUARIO_ID, inicio, fim);

        assertThat(saldo.totalReceitas()).isEqualByComparingTo("500.00");
        assertThat(saldo.totalDespesas()).isEqualByComparingTo("200.00");
        assertThat(saldo.saldo()).isEqualByComparingTo("300.00");
        assertThat(saldo.dataInicio()).isEqualTo(inicio);
        assertThat(saldo.dataFim()).isEqualTo(fim);
    }

    @Test
    void calcularSaldo_dataInicioPosteriorADataFim_lancaExcecao() {
        LocalDate inicio = LocalDate.of(2026, 2, 1);
        LocalDate fim = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> relatorioService.calcularSaldo(USUARIO_ID, inicio, fim))
                .isInstanceOf(IntervaloDataInvalidoException.class);
    }

    @Test
    void calcularSaldo_apenasUmaDataInformada_lancaExcecao() {
        assertThatThrownBy(() -> relatorioService.calcularSaldo(USUARIO_ID, LocalDate.now(), null))
                .isInstanceOf(IntervaloDataInvalidoException.class);
    }
}
