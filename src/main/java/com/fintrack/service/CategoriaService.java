package com.fintrack.service;

import com.fintrack.dto.CategoriaDTO;
import com.fintrack.exception.CategoriaComTransacoesException;
import com.fintrack.exception.CategoriaGlobalImutavelException;
import com.fintrack.exception.CategoriaNaoEncontradaException;
import com.fintrack.exception.NomeCategoriaDuplicadoException;
import com.fintrack.model.Categoria;
import com.fintrack.model.Usuario;
import com.fintrack.repository.CategoriaRepository;
import com.fintrack.repository.TransacaoRepository;
import com.fintrack.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final TransacaoRepository transacaoRepository;
    private final UsuarioRepository usuarioRepository;

    public CategoriaService(CategoriaRepository categoriaRepository,
                             TransacaoRepository transacaoRepository,
                             UsuarioRepository usuarioRepository) {
        this.categoriaRepository = categoriaRepository;
        this.transacaoRepository = transacaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaDTO> listar(Long usuarioId) {
        return categoriaRepository.findVisiveisPara(usuarioId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public CategoriaDTO criar(Long usuarioId, CategoriaDTO dto) {
        validarNomeDisponivel(dto.nome(), usuarioId);

        Usuario usuario = usuarioRepository.getReferenceById(usuarioId);
        Categoria categoria = new Categoria(dto.nome(), usuario);
        categoriaRepository.save(categoria);

        return toDTO(categoria);
    }

    @Transactional
    public CategoriaDTO atualizar(Long usuarioId, Long id, CategoriaDTO dto) {
        Categoria categoria = buscarPropriaEditavel(usuarioId, id);

        if (!categoria.getNome().equalsIgnoreCase(dto.nome())) {
            validarNomeDisponivel(dto.nome(), usuarioId);
        }
        categoria.setNome(dto.nome());

        return toDTO(categoria);
    }

    @Transactional
    public void excluir(Long usuarioId, Long id) {
        Categoria categoria = buscarPropriaEditavel(usuarioId, id);

        if (transacaoRepository.existsByCategoriaId(categoria.getId())) {
            throw new CategoriaComTransacoesException();
        }
        categoriaRepository.delete(categoria);
    }

    /**
     * Busca uma categoria visivel ao usuario (global ou propria) e barra
     * a global com 403 — quem chega aqui ja sabe que o id existe, so nao
     * pode mexer nele. Um id de outro usuario nunca chega aqui: nao e
     * global nem do dono, entao findAcessivelPor nao o retorna e vira 404.
     */
    private Categoria buscarPropriaEditavel(Long usuarioId, Long id) {
        Categoria categoria = categoriaRepository.findAcessivelPor(id, usuarioId)
                .orElseThrow(() -> new CategoriaNaoEncontradaException(id));

        if (categoria.isGlobal()) {
            throw new CategoriaGlobalImutavelException();
        }
        return categoria;
    }

    private void validarNomeDisponivel(String nome, Long usuarioId) {
        boolean colideComGlobal = categoriaRepository.existsByNomeIgnoreCaseAndUsuarioIsNull(nome);
        boolean colideComPropria = categoriaRepository.existsByNomeIgnoreCaseAndUsuarioId(nome, usuarioId);

        if (colideComGlobal || colideComPropria) {
            throw new NomeCategoriaDuplicadoException(nome);
        }
    }

    private CategoriaDTO toDTO(Categoria categoria) {
        return new CategoriaDTO(categoria.getId(), categoria.getNome(), categoria.isGlobal());
    }
}
