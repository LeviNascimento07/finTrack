package com.fintrack.controller;

import com.fintrack.dto.LoginDTO;
import com.fintrack.dto.RegistroDTO;
import com.fintrack.dto.TokenDTO;
import com.fintrack.exception.CredenciaisInvalidasException;
import com.fintrack.exception.EmailJaCadastradoException;
import com.fintrack.exception.ErroResponse;
import com.fintrack.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<TokenDTO> register(@Valid @RequestBody RegistroDTO dto) {
        TokenDTO token = authService.registrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenDTO> login(@Valid @RequestBody LoginDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    // Handlers locais: nao ha GlobalExceptionHandler ate a etapa 5.
    // Mesmo formato de ErroResponse que o handler global vai reutilizar.

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ErroResponse> handleEmailJaCadastrado(EmailJaCadastradoException ex,
                                                                  HttpServletRequest request) {
        return construirErro(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResponse> handleCredenciaisInvalidas(CredenciaisInvalidasException ex,
                                                                     HttpServletRequest request) {
        return construirErro(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    private ResponseEntity<ErroResponse> construirErro(HttpStatus status, String mensagem, HttpServletRequest request) {
        ErroResponse erro = new ErroResponse(Instant.now(), status.value(), mensagem, request.getRequestURI());
        return ResponseEntity.status(status).body(erro);
    }
}
