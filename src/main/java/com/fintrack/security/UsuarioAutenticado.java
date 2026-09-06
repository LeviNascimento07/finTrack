package com.fintrack.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Uso restrito a services que nao recebem @AuthenticationPrincipal (a forma
 * padrao nos controllers). O principal de uma requisicao anonima e a String
 * "anonymousUser", nao um UsuarioDetailsImpl — por isso a guarda abaixo.
 */
public final class UsuarioAutenticado {

    private UsuarioAutenticado() {
    }

    public static Long getId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UsuarioDetailsImpl usuarioDetails)) {
            throw new IllegalStateException("Nenhum usuário autenticado no contexto de segurança");
        }

        return usuarioDetails.getId();
    }
}
