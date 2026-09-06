package com.fintrack.service;

import com.fintrack.dto.LoginDTO;
import com.fintrack.dto.RegistroDTO;
import com.fintrack.dto.TokenDTO;
import com.fintrack.exception.CredenciaisInvalidasException;
import com.fintrack.exception.EmailJaCadastradoException;
import com.fintrack.model.Usuario;
import com.fintrack.repository.UsuarioRepository;
import com.fintrack.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository,
                        PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager,
                        JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public TokenDTO registrar(RegistroDTO dto) {
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new EmailJaCadastradoException(dto.email());
        }

        Usuario usuario = new Usuario(dto.nome(), dto.email(), passwordEncoder.encode(dto.senha()));
        usuarioRepository.save(usuario);

        return new TokenDTO(jwtService.gerarToken(usuario.getEmail()));
    }

    public TokenDTO login(LoginDTO dto) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.email(), dto.senha()));
        } catch (AuthenticationException e) {
            throw new CredenciaisInvalidasException();
        }

        return new TokenDTO(jwtService.gerarToken(dto.email()));
    }
}
