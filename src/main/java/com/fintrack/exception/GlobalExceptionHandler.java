package com.fintrack.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Unico ponto de mapeamento excecao -> HTTP status da API. Todo o corpo de
 * erro sai no formato de ErroResponse, o mesmo usado por
 * JwtAuthenticationEntryPoint (401) e JwtAccessDeniedHandler (403) — esses
 * dois continuam separados porque rodam no filtro de seguranca, antes do
 * DispatcherServlet, e por isso nunca chegam aqui.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErroCampoDTO> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ErroCampoDTO(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        return construirErro(HttpStatus.BAD_REQUEST, "Erro de validação", request, campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<ErroCampoDTO> campos = ex.getConstraintViolations().stream()
                .map(violation -> new ErroCampoDTO(violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return construirErro(HttpStatus.BAD_REQUEST, "Erro de validação", request, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> handleMensagemIlegivel(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return construirErro(HttpStatus.BAD_REQUEST, "Corpo da requisição ausente, malformado ou com valor inválido", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> handleTipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String mensagem = "Parâmetro '" + ex.getName() + "' com valor inválido";
        return construirErro(HttpStatus.BAD_REQUEST, mensagem, request);
    }

    @ExceptionHandler(IntervaloDataInvalidoException.class)
    public ResponseEntity<ErroResponse> handleIntervaloDataInvalido(IntervaloDataInvalidoException ex, HttpServletRequest request) {
        return construirErro(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler({
            RecursoNaoEncontradoException.class,
            CategoriaNaoEncontradaException.class,
            TransacaoNaoEncontradaException.class
    })
    public ResponseEntity<ErroResponse> handleNaoEncontrado(RuntimeException ex, HttpServletRequest request) {
        return construirErro(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(CategoriaGlobalImutavelException.class)
    public ResponseEntity<ErroResponse> handleCategoriaGlobalImutavel(CategoriaGlobalImutavelException ex, HttpServletRequest request) {
        return construirErro(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResponse> handleCredenciaisInvalidas(CredenciaisInvalidasException ex, HttpServletRequest request) {
        return construirErro(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler({
            SaldoInsuficienteException.class,
            EmailJaCadastradoException.class,
            NomeCategoriaDuplicadoException.class,
            CategoriaComTransacoesException.class
    })
    public ResponseEntity<ErroResponse> handleConflito(RuntimeException ex, HttpServletRequest request) {
        return construirErro(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /**
     * Fallback: qualquer excecao nao mapeada. NUNCA expor ex.getMessage(),
     * stack trace ou nome de classe ao cliente — só no log do servidor.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleFallback(Exception ex, HttpServletRequest request) {
        log.error("Erro não tratado em {} {}", request.getMethod(), request.getRequestURI(), ex);
        return construirErro(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", request);
    }

    private ResponseEntity<ErroResponse> construirErro(HttpStatus status, String mensagem, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ErroResponse.of(status, mensagem, request.getRequestURI()));
    }

    private ResponseEntity<ErroResponse> construirErro(HttpStatus status, String mensagem, HttpServletRequest request,
                                                         List<ErroCampoDTO> campos) {
        return ResponseEntity.status(status).body(ErroResponse.of(status, mensagem, request.getRequestURI(), campos));
    }
}
