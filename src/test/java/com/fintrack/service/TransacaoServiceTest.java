package com.fintrack.service;

import com.fintrack.dto.TransacaoRequestDTO;
import com.fintrack.dto.TransacaoResponseDTO;
import com.fintrack.exception.SaldoInsuficienteException;
import com.fintrack.model.Categoria;
import com.fintrack.model.TipoTransacao;
import com.fintrack.model.Transacao;
import com.fintrack.model.Usuario;
import com.fintrack.repository.CategoriaRepository;
import com.fintrack.repository.TransacaoRepository;
import com.fintrack.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransacaoServiceTest {

    private static final Long USUARIO_ID = 1L;
    private static final Long CATEGORIA_ID = 10L;

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private TransacaoService transacaoService;

    private Usuario usuario;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("Usuário Teste", "teste@teste.com", "hash");
        categoria = new Categoria("Categoria Teste", null);

        when(categoriaRepository.findAcessivelPor(CATEGORIA_ID, USUARIO_ID)).thenReturn(Optional.of(categoria));
    }

    @Test
    void criar_comSaldoSuficiente_naoLancaExcecaoEPersiste() {
        when(transacaoRepository.sumValorByUsuarioIdAndTipo(USUARIO_ID, TipoTransacao.RECEITA))
                .thenReturn(new BigDecimal("200"));
        when(transacaoRepository.sumValorByUsuarioIdAndTipo(USUARIO_ID, TipoTransacao.DESPESA))
                .thenReturn(new BigDecimal("100"));
        when(usuarioRepository.getReferenceById(USUARIO_ID)).thenReturn(usuario);

        TransacaoRequestDTO dto = new TransacaoRequestDTO(
                "Compra", new BigDecimal("50"), TipoTransacao.DESPESA, LocalDate.now(), CATEGORIA_ID);

        TransacaoResponseDTO resultado = transacaoService.criar(USUARIO_ID, dto);

        assertThat(resultado.valor()).isEqualByComparingTo("50");
        assertThat(resultado.tipo()).isEqualTo(TipoTransacao.DESPESA);
        verify(transacaoRepository).save(any(Transacao.class));
    }

    @Test
    void criar_comSaldoInsuficiente_lancaSaldoInsuficienteExceptionENaoPersiste() {
        // receitas 100 - despesas 100 = saldo 0; nova despesa de 10 deixaria negativo.
        when(transacaoRepository.sumValorByUsuarioIdAndTipo(USUARIO_ID, TipoTransacao.RECEITA))
                .thenReturn(new BigDecimal("100"));
        when(transacaoRepository.sumValorByUsuarioIdAndTipo(USUARIO_ID, TipoTransacao.DESPESA))
                .thenReturn(new BigDecimal("100"));

        TransacaoRequestDTO dto = new TransacaoRequestDTO(
                "Compra", new BigDecimal("10"), TipoTransacao.DESPESA, LocalDate.now(), CATEGORIA_ID);

        assertThatThrownBy(() -> transacaoService.criar(USUARIO_ID, dto))
                .isInstanceOf(SaldoInsuficienteException.class);

        verify(transacaoRepository, never()).save(any());
    }

    @Test
    void atualizar_despesaPropria_desconsideraValorAntigoAoChecarSaldo() {
        // Cenario do bug: receita total 150, despesas totais 100 (so a desta transacao,
        // que esta sendo editada de 100 para 101). Sem desconsiderar o valor antigo, o
        // saldo (150-100=50) menos o novo valor (101) daria -51 e rejeitaria indevidamente.
        // Desconsiderando: saldo sem esta transacao = 150 (nao ha outras despesas); 150-101=49 >= 0.
        Transacao transacaoExistente = new Transacao(
                "Antiga", new BigDecimal("100"), TipoTransacao.DESPESA, LocalDate.now(), usuario, categoria);
        Long transacaoId = 99L;

        when(transacaoRepository.findByIdAndUsuarioId(transacaoId, USUARIO_ID))
                .thenReturn(Optional.of(transacaoExistente));
        when(transacaoRepository.sumValorByUsuarioIdAndTipo(USUARIO_ID, TipoTransacao.RECEITA))
                .thenReturn(new BigDecimal("150"));
        when(transacaoRepository.sumValorByUsuarioIdAndTipo(USUARIO_ID, TipoTransacao.DESPESA))
                .thenReturn(new BigDecimal("100"));

        TransacaoRequestDTO dto = new TransacaoRequestDTO(
                "Editada", new BigDecimal("101"), TipoTransacao.DESPESA, LocalDate.now(), CATEGORIA_ID);

        TransacaoResponseDTO resultado = transacaoService.atualizar(USUARIO_ID, transacaoId, dto);

        assertThat(resultado.valor()).isEqualByComparingTo("101");
    }
}
