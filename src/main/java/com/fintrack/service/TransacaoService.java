package com.fintrack.service;

import com.fintrack.dto.TransacaoRequestDTO;
import com.fintrack.dto.TransacaoResponseDTO;
import com.fintrack.exception.CategoriaNaoEncontradaException;
import com.fintrack.exception.SaldoInsuficienteException;
import com.fintrack.exception.TransacaoNaoEncontradaException;
import com.fintrack.model.Categoria;
import com.fintrack.model.TipoTransacao;
import com.fintrack.model.Transacao;
import com.fintrack.model.Usuario;
import com.fintrack.repository.CategoriaRepository;
import com.fintrack.repository.TransacaoRepository;
import com.fintrack.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public TransacaoService(TransacaoRepository transacaoRepository,
                             CategoriaRepository categoriaRepository,
                             UsuarioRepository usuarioRepository) {
        this.transacaoRepository = transacaoRepository;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public Page<TransacaoResponseDTO> listar(Long usuarioId, Long categoriaId, LocalDate dataInicio,
                                              LocalDate dataFim, Pageable pageable) {
        return transacaoRepository.findComFiltros(usuarioId, categoriaId, dataInicio, dataFim, pageable)
                .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public TransacaoResponseDTO buscar(Long usuarioId, Long id) {
        return toDTO(buscarPropria(usuarioId, id));
    }

    @Transactional
    public TransacaoResponseDTO criar(Long usuarioId, TransacaoRequestDTO dto) {
        Categoria categoria = categoriaAcessivel(usuarioId, dto.categoriaId());
        validarSaldoSuficiente(usuarioId, dto.tipo(), dto.valor(), null);

        Usuario usuario = usuarioRepository.getReferenceById(usuarioId);
        Transacao transacao = new Transacao(dto.descricao(), dto.valor(), dto.tipo(), dto.data(), usuario, categoria);
        transacaoRepository.save(transacao);

        return toDTO(transacao);
    }

    @Transactional
    public TransacaoResponseDTO atualizar(Long usuarioId, Long id, TransacaoRequestDTO dto) {
        Transacao transacao = buscarPropria(usuarioId, id);
        Categoria categoria = categoriaAcessivel(usuarioId, dto.categoriaId());
        validarSaldoSuficiente(usuarioId, dto.tipo(), dto.valor(), transacao);

        transacao.setDescricao(dto.descricao());
        transacao.setValor(dto.valor());
        transacao.setTipo(dto.tipo());
        transacao.setData(dto.data());
        transacao.setCategoria(categoria);

        return toDTO(transacao);
    }

    @Transactional
    public void excluir(Long usuarioId, Long id) {
        transacaoRepository.delete(buscarPropria(usuarioId, id));
    }

    /**
     * Saldo = receitas - despesas do usuario, sem recorte de data. So
     * despesa pode deixar o saldo negativo, por isso o metodo nao faz nada
     * quando o novo tipo e RECEITA.
     *
     * Na edicao (transacaoAtual != null), a contribuicao ORIGINAL da
     * transacao sendo editada e removida do saldo antes de aplicar o novo
     * valor/tipo — senao editar uma despesa de 100 para 101 seria
     * rejeitado (o saldo contaria os 100 antigos E os 101 novos).
     */
    private void validarSaldoSuficiente(Long usuarioId, TipoTransacao novoTipo, BigDecimal novoValor,
                                         Transacao transacaoAtual) {
        if (novoTipo != TipoTransacao.DESPESA) {
            return;
        }

        BigDecimal receitas = transacaoRepository.sumValorByUsuarioIdAndTipo(usuarioId, TipoTransacao.RECEITA);
        BigDecimal despesas = transacaoRepository.sumValorByUsuarioIdAndTipo(usuarioId, TipoTransacao.DESPESA);
        BigDecimal saldoAtual = receitas.subtract(despesas);

        if (transacaoAtual != null) {
            BigDecimal contribuicaoAntiga = transacaoAtual.getTipo() == TipoTransacao.RECEITA
                    ? transacaoAtual.getValor()
                    : transacaoAtual.getValor().negate();
            saldoAtual = saldoAtual.subtract(contribuicaoAntiga);
        }

        if (saldoAtual.subtract(novoValor).signum() < 0) {
            throw new SaldoInsuficienteException();
        }
    }

    private Transacao buscarPropria(Long usuarioId, Long id) {
        return transacaoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new TransacaoNaoEncontradaException(id));
    }

    private Categoria categoriaAcessivel(Long usuarioId, Long categoriaId) {
        return categoriaRepository.findAcessivelPor(categoriaId, usuarioId)
                .orElseThrow(() -> new CategoriaNaoEncontradaException(categoriaId));
    }

    private TransacaoResponseDTO toDTO(Transacao transacao) {
        return new TransacaoResponseDTO(
                transacao.getId(),
                transacao.getDescricao(),
                transacao.getValor(),
                transacao.getTipo(),
                transacao.getData(),
                transacao.getCategoria().getId(),
                transacao.getCategoria().getNome());
    }
}
