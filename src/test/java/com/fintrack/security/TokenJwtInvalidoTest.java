package com.fintrack.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Complementa TransacaoAcessoCruzadoTest (isolamento por usuario, para
 * quem JA tem um token valido) cobrindo o outro lado da autenticacao: um
 * token que nao deveria nem chegar a identificar ninguem. Os tres casos
 * caem no JwtAuthenticationEntryPoint (401) — "nao sei quem voce e" — e
 * nunca no AccessDeniedHandler (403), que e "sei quem voce e, mas nao
 * pode". Nao depende de nenhum usuario/dado persistido: o
 * JwtAuthenticationFilter rejeita o token antes de consultar o banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TokenJwtInvalidoTest {

    private static final String ROTA_PROTEGIDA = "/api/v1/usuarios/1/categorias";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void tokenExpirado_retorna401() throws Exception {
        SecretKey chave = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        Instant expirouHaUmaHora = Instant.now().minusSeconds(3600);

        String tokenExpirado = Jwts.builder()
                .subject("qualquer@teste.com")
                .issuedAt(Date.from(expirouHaUmaHora.minusSeconds(3600)))
                .expiration(Date.from(expirouHaUmaHora))
                .signWith(chave)
                .compact();

        mockMvc.perform(get(ROTA_PROTEGIDA).header("Authorization", "Bearer " + tokenExpirado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenComAssinaturaInvalida_retorna401() throws Exception {
        SecretKey chaveErrada = Keys.hmacShaKeyFor(
                "outro-segredo-completamente-diferente-do-real".getBytes(StandardCharsets.UTF_8));
        Instant agora = Instant.now();

        String tokenAssinaturaInvalida = Jwts.builder()
                .subject("qualquer@teste.com")
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusSeconds(3600)))
                .signWith(chaveErrada)
                .compact();

        mockMvc.perform(get(ROTA_PROTEGIDA).header("Authorization", "Bearer " + tokenAssinaturaInvalida))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void headerAuthorizationSemPrefixoBearer_retorna401() throws Exception {
        mockMvc.perform(get(ROTA_PROTEGIDA).header("Authorization", "token-sem-prefixo-bearer"))
                .andExpect(status().isUnauthorized());
    }
}
