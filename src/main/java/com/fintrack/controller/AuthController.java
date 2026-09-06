package com.fintrack.controller;

import com.fintrack.dto.LoginDTO;
import com.fintrack.dto.RegistroDTO;
import com.fintrack.dto.TokenDTO;
import com.fintrack.exception.ErroResponse;
import com.fintrack.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticação", description = "Registro e login — os únicos endpoints públicos da API, não exigem token")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Registrar novo usuário",
            description = "Cria um usuário e devolve um token JWT já autenticado, pronto para uso nos demais endpoints."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso",
                    content = @Content(schema = @Schema(implementation = TokenDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados de registro inválidos (nome/e-mail/senha)",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<TokenDTO> register(@Valid @RequestBody RegistroDTO dto) {
        TokenDTO token = authService.registrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }

    @Operation(
            summary = "Autenticar usuário",
            description = "Valida e-mail e senha e devolve um token JWT."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login bem-sucedido",
                    content = @Content(schema = @Schema(implementation = TokenDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados de login inválidos (e-mail/senha ausentes)",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "401", description = "E-mail ou senha incorretos — mensagem genérica, "
                    + "não revela qual dos dois",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<TokenDTO> login(@Valid @RequestBody LoginDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }
}
