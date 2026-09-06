package com.fintrack.controller;

import com.fintrack.dto.CategoriaDTO;
import com.fintrack.exception.CategoriaComTransacoesException;
import com.fintrack.exception.CategoriaGlobalImutavelException;
import com.fintrack.exception.CategoriaNaoEncontradaException;
import com.fintrack.exception.ErroResponse;
import com.fintrack.exception.NomeCategoriaDuplicadoException;
import com.fintrack.exception.RecursoNaoEncontradoException;
import com.fintrack.security.UsuarioDetailsImpl;
import com.fintrack.service.CategoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios/{usuarioId}/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public List<CategoriaDTO> listar(@PathVariable Long usuarioId,
                                      @AuthenticationPrincipal UsuarioDetailsImpl usuarioLogado) {
        validarUsuarioDaRota(usuarioId, usuarioLogado);
        return categoriaService.listar(usuarioId);
    }

    @PostMapping
    public ResponseEntity<CategoriaDTO> criar(@PathVariable Long usuarioId,
                                               @AuthenticationPrincipal UsuarioDetailsImpl usuarioLogado,
                                               @Valid @RequestBody CategoriaDTO dto) {
        validarUsuarioDaRota(usuarioId, usuarioLogado);
        CategoriaDTO criada = categoriaService.criar(usuarioId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @PutMapping("/{id}")
    public CategoriaDTO atualizar(@PathVariable Long usuarioId,
                                   @PathVariable Long id,
                                   @AuthenticationPrincipal UsuarioDetailsImpl usuarioLogado,
                                   @Valid @RequestBody CategoriaDTO dto) {
        validarUsuarioDaRota(usuarioId, usuarioLogado);
        return categoriaService.atualizar(usuarioId, id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long usuarioId,
                                         @PathVariable Long id,
                                         @AuthenticationPrincipal UsuarioDetailsImpl usuarioLogado) {
        validarUsuarioDaRota(usuarioId, usuarioLogado);
        categoriaService.excluir(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * {usuarioId} da rota tem que ser o do token — se nao bater, 404
     * (nunca 403: confirmar que o id pertence a outra pessoa ja seria vazar
     * informacao).
     */
    private void validarUsuarioDaRota(Long usuarioId, UsuarioDetailsImpl usuarioLogado) {
        if (!usuarioId.equals(usuarioLogado.getId())) {
            throw new RecursoNaoEncontradoException();
        }
    }

    // Handlers locais: nao ha GlobalExceptionHandler ate a etapa 5.
    // Mesmo formato de ErroResponse usado no AuthController.

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex,
                                                                     HttpServletRequest request) {
        return construirErro(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(CategoriaNaoEncontradaException.class)
    public ResponseEntity<ErroResponse> handleCategoriaNaoEncontrada(CategoriaNaoEncontradaException ex,
                                                                       HttpServletRequest request) {
        return construirErro(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(CategoriaGlobalImutavelException.class)
    public ResponseEntity<ErroResponse> handleCategoriaGlobalImutavel(CategoriaGlobalImutavelException ex,
                                                                        HttpServletRequest request) {
        return construirErro(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(NomeCategoriaDuplicadoException.class)
    public ResponseEntity<ErroResponse> handleNomeDuplicado(NomeCategoriaDuplicadoException ex,
                                                               HttpServletRequest request) {
        return construirErro(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(CategoriaComTransacoesException.class)
    public ResponseEntity<ErroResponse> handleCategoriaComTransacoes(CategoriaComTransacoesException ex,
                                                                        HttpServletRequest request) {
        return construirErro(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    private ResponseEntity<ErroResponse> construirErro(HttpStatus status, String mensagem, HttpServletRequest request) {
        ErroResponse erro = new ErroResponse(Instant.now(), status.value(), mensagem, request.getRequestURI());
        return ResponseEntity.status(status).body(erro);
    }
}
