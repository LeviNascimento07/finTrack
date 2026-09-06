package com.fintrack.controller;

import com.fintrack.dto.SaldoDTO;
import com.fintrack.dto.TotalPorCategoriaDTO;
import com.fintrack.exception.ErroResponse;
import com.fintrack.exception.IntervaloDataInvalidoException;
import com.fintrack.exception.RecursoNaoEncontradoException;
import com.fintrack.model.TipoTransacao;
import com.fintrack.security.UsuarioDetailsImpl;
import com.fintrack.service.RelatorioService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios/{usuarioId}")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/saldo")
    public SaldoDTO saldo(@PathVariable Long usuarioId,
                           @AuthenticationPrincipal UsuarioDetailsImpl usuarioLogado,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        validarUsuarioDaRota(usuarioId, usuarioLogado);
        return relatorioService.calcularSaldo(usuarioId, dataInicio, dataFim);
    }

    @GetMapping("/relatorios/por-categoria")
    public List<TotalPorCategoriaDTO> porCategoria(@PathVariable Long usuarioId,
                                                     @AuthenticationPrincipal UsuarioDetailsImpl usuarioLogado,
                                                     @RequestParam(defaultValue = "DESPESA") TipoTransacao tipo) {
        validarUsuarioDaRota(usuarioId, usuarioLogado);
        return relatorioService.totalizarPorCategoria(usuarioId, tipo);
    }

    private void validarUsuarioDaRota(Long usuarioId, UsuarioDetailsImpl usuarioLogado) {
        if (!usuarioId.equals(usuarioLogado.getId())) {
            throw new RecursoNaoEncontradoException();
        }
    }

    // Handlers locais: nao ha GlobalExceptionHandler ate a etapa 5 (ver docs/CONVENCOES.md).

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex,
                                                                     HttpServletRequest request) {
        return construirErro(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(IntervaloDataInvalidoException.class)
    public ResponseEntity<ErroResponse> handleIntervaloDataInvalido(IntervaloDataInvalidoException ex,
                                                                       HttpServletRequest request) {
        return construirErro(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    private ResponseEntity<ErroResponse> construirErro(HttpStatus status, String mensagem, HttpServletRequest request) {
        ErroResponse erro = new ErroResponse(Instant.now(), status.value(), mensagem, request.getRequestURI());
        return ResponseEntity.status(status).body(erro);
    }
}
