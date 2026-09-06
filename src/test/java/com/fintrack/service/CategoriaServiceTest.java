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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    private static final Long USUARIO_ID = 1L;
    private static final Long CATEGORIA_ID = 9L;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("Usuário Teste", "teste@teste.com", "hash");
    }

    @Test
    void criar_nomeDuplicadoContraPropria_lancaExcecaoENaoPersiste() {
        when(categoriaRepository.existsByNomeIgnoreCaseAndUsuarioIsNull("Viagens")).thenReturn(false);
        when(categoriaRepository.existsByNomeIgnoreCaseAndUsuarioId("Viagens", USUARIO_ID)).thenReturn(true);

        CategoriaDTO dto = new CategoriaDTO(null, "Viagens", false);

        assertThatThrownBy(() -> categoriaService.criar(USUARIO_ID, dto))
                .isInstanceOf(NomeCategoriaDuplicadoException.class);

        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void criar_nomeDuplicadoContraGlobal_lancaExcecaoENaoPersiste() {
        when(categoriaRepository.existsByNomeIgnoreCaseAndUsuarioIsNull("Lazer")).thenReturn(true);

        CategoriaDTO dto = new CategoriaDTO(null, "Lazer", false);

        assertThatThrownBy(() -> categoriaService.criar(USUARIO_ID, dto))
                .isInstanceOf(NomeCategoriaDuplicadoException.class);

        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void atualizar_categoriaGlobal_lancaCategoriaGlobalImutavelException() {
        Categoria categoriaGlobal = new Categoria("Lazer", null);
        when(categoriaRepository.findAcessivelPor(CATEGORIA_ID, USUARIO_ID)).thenReturn(Optional.of(categoriaGlobal));

        CategoriaDTO dto = new CategoriaDTO(null, "Lazer Editado", false);

        assertThatThrownBy(() -> categoriaService.atualizar(USUARIO_ID, CATEGORIA_ID, dto))
                .isInstanceOf(CategoriaGlobalImutavelException.class);
    }

    @Test
    void excluir_categoriaGlobal_lancaCategoriaGlobalImutavelExceptionENaoDeleta() {
        Categoria categoriaGlobal = new Categoria("Lazer", null);
        when(categoriaRepository.findAcessivelPor(CATEGORIA_ID, USUARIO_ID)).thenReturn(Optional.of(categoriaGlobal));

        assertThatThrownBy(() -> categoriaService.excluir(USUARIO_ID, CATEGORIA_ID))
                .isInstanceOf(CategoriaGlobalImutavelException.class);

        verify(categoriaRepository, never()).delete(any());
    }

    @Test
    void excluir_categoriaComTransacoesAssociadas_lancaExcecaoENaoDeleta() {
        Categoria categoriaPropria = new Categoria("Viagens", usuario);
        when(categoriaRepository.findAcessivelPor(CATEGORIA_ID, USUARIO_ID)).thenReturn(Optional.of(categoriaPropria));
        when(transacaoRepository.existsByCategoriaId(any())).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.excluir(USUARIO_ID, CATEGORIA_ID))
                .isInstanceOf(CategoriaComTransacoesException.class);

        verify(categoriaRepository, never()).delete(any());
    }

    @Test
    void atualizar_categoriaDeOutroUsuario_lancaCategoriaNaoEncontradaException() {
        // findAcessivelPor so retorna global ou propria; id de outro usuario
        // nao aparece, entao o mock reflete isso com Optional.empty().
        when(categoriaRepository.findAcessivelPor(CATEGORIA_ID, USUARIO_ID)).thenReturn(Optional.empty());

        CategoriaDTO dto = new CategoriaDTO(null, "Qualquer Nome", false);

        assertThatThrownBy(() -> categoriaService.atualizar(USUARIO_ID, CATEGORIA_ID, dto))
                .isInstanceOf(CategoriaNaoEncontradaException.class);
    }

    @Test
    void listar_trazGlobaisEPropriasComFlagGlobalCorreta() {
        Categoria global = new Categoria("Alimentação", null);
        Categoria propria = new Categoria("Viagens", usuario);
        when(categoriaRepository.findVisiveisPara(USUARIO_ID)).thenReturn(List.of(global, propria));

        List<CategoriaDTO> resultado = categoriaService.listar(USUARIO_ID);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).nome()).isEqualTo("Alimentação");
        assertThat(resultado.get(0).global()).isTrue();
        assertThat(resultado.get(1).nome()).isEqualTo("Viagens");
        assertThat(resultado.get(1).global()).isFalse();
    }
}
