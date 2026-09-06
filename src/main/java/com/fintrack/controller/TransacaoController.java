package com.fintrack.controller;

import com.fintrack.dto.TransacaoRequestDTO;
import com.fintrack.dto.TransacaoResponseDTO;
import com.fintrack.exception.RecursoNaoEncontradoException;
import com.fintrack.security.UsuarioDetailsImpl;
import com.fintrack.service.TransacaoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/usuarios/{usuarioId}/transacoes")
public class TransacaoController {

    private final TransacaoService transacaoService;

    public TransacaoController(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    @GetMapping
    public Page<TransacaoResponseDTO> listar(@PathVariable Long usuarioId,
                                              @AuthenticationPrincipal UsuarioDetailsImpl usuarioLogado,
                                              @RequestParam(required = false) Long categoriaId,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
                                              @PageableDefault(size = 20, sort = "data", direction = Sort.Direction.DESC) Pageable pageable) {
        validarUsuarioDaRota(usuarioId, usuarioLogado);
        return transacaoService.listar(usuarioId, categoriaId, dataInicio, dataFim, pageable);
    }

    @GetMapping("/{id}")
    public TransacaoResponseDTO buscar(@PathVariable Long usuarioId,
                                        @PathVariable Long id,
                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioLogado) {
        validarUsuarioDaRota(usuarioId, usuarioLogado);
        return transacaoService.buscar(usuarioId, id);
    }

    @PostMapping
    public ResponseEntity<TransacaoResponseDTO> criar(@PathVariable Long usuarioId,
                                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioLogado,
                                                        @Valid @RequestBody TransacaoRequestDTO dto) {
        validarUsuarioDaRota(usuarioId, usuarioLogado);
        TransacaoResponseDTO criada = transacaoService.criar(usuarioId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @PutMapping("/{id}")
    public TransacaoResponseDTO atualizar(@PathVariable Long usuarioId,
                                           @PathVariable Long id,
                                           @AuthenticationPrincipal UsuarioDetailsImpl usuarioLogado,
                                           @Valid @RequestBody TransacaoRequestDTO dto) {
        validarUsuarioDaRota(usuarioId, usuarioLogado);
        return transacaoService.atualizar(usuarioId, id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long usuarioId,
                                         @PathVariable Long id,
                                         @AuthenticationPrincipal UsuarioDetailsImpl usuarioLogado) {
        validarUsuarioDaRota(usuarioId, usuarioLogado);
        transacaoService.excluir(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    private void validarUsuarioDaRota(Long usuarioId, UsuarioDetailsImpl usuarioLogado) {
        if (!usuarioId.equals(usuarioLogado.getId())) {
            throw new RecursoNaoEncontradoException();
        }
    }
}
