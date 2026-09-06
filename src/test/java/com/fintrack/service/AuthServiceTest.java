package com.fintrack.service;

import com.fintrack.dto.LoginDTO;
import com.fintrack.dto.RegistroDTO;
import com.fintrack.exception.CredenciaisInvalidasException;
import com.fintrack.exception.EmailJaCadastradoException;
import com.fintrack.repository.UsuarioRepository;
import com.fintrack.security.JwtProperties;
import com.fintrack.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JwtService nao e mockado (e uma classe concreta, nao interface — o
 * Mockito "inline mock maker"/ByteBuddy deste projeto nao consegue
 * instrumentar classes concretas no JDK 24 usado neste ambiente). Como
 * nenhum destes tres cenarios chega a chamar JwtService (a excecao
 * acontece antes), uma instancia real e suficiente e evita o problema.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtService jwtService = new JwtService(new JwtProperties(
                "segredo-de-teste-com-32-caracteres-ou-mais-apenas-para-testes", 3600000L));
        authService = new AuthService(usuarioRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void registrar_emailJaCadastrado_lancaExcecaoENaoPersiste() {
        RegistroDTO dto = new RegistroDTO("Novo Usuário", "existente@teste.com", "senha123");
        when(usuarioRepository.existsByEmail("existente@teste.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(dto))
                .isInstanceOf(EmailJaCadastradoException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void login_senhaErrada_lancaCredenciaisInvalidasComMensagemGenerica() {
        LoginDTO dto = new LoginDTO("usuario@teste.com", "senhaErrada");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("Credenciais inválidas");
    }

    @Test
    void login_emailInexistente_lancaOMesmoErroGenericoQueSenhaErrada() {
        // UsernameNotFoundException e AuthenticationException — o
        // AuthenticationManager real lanca essa excecao quando o
        // UserDetailsService nao acha o e-mail. O service precisa tratar
        // igual a uma senha errada: mesma excecao, mesma mensagem, sem
        // revelar se o e-mail existe.
        LoginDTO dto = new LoginDTO("naoexiste@teste.com", "qualquerSenha1");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new UsernameNotFoundException("Usuário não encontrado: naoexiste@teste.com"));

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("Credenciais inválidas");
    }
}
