package com.fintrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.dto.TransacaoRequestDTO;
import com.fintrack.model.Categoria;
import com.fintrack.model.TipoTransacao;
import com.fintrack.model.Transacao;
import com.fintrack.model.Usuario;
import com.fintrack.repository.CategoriaRepository;
import com.fintrack.repository.TransacaoRepository;
import com.fintrack.repository.UsuarioRepository;
import com.fintrack.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prova que o usuario A nunca enxerga uma transacao do usuario B, mesmo
 * acessando com o seu proprio {usuarioId} na rota (o cenario real de IDOR:
 * adivinhar o id da transacao de outra pessoa). Ver "regra de ouro dos
 * repositories" em CLAUDE.md.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransacaoAcessoCruzadoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Long usuarioAId;
    private String tokenA;
    private Long transacaoBId;
    private Long categoriaId;

    @BeforeEach
    void setUp() {
        Usuario usuarioA = usuarioRepository.save(
                new Usuario("Usuário A", "a-" + UUID.randomUUID() + "@teste.com", passwordEncoder.encode("senha123")));
        Usuario usuarioB = usuarioRepository.save(
                new Usuario("Usuário B", "b-" + UUID.randomUUID() + "@teste.com", passwordEncoder.encode("senha123")));

        usuarioAId = usuarioA.getId();
        tokenA = jwtService.gerarToken(usuarioA.getEmail());

        Categoria categoriaGlobal = categoriaRepository.findVisiveisPara(usuarioB.getId()).stream()
                .filter(Categoria::isGlobal)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhuma categoria global semeada pelo DataInitializer"));
        categoriaId = categoriaGlobal.getId();

        Transacao transacaoB = transacaoRepository.save(new Transacao(
                "Gasto do usuário B", new BigDecimal("50.00"), TipoTransacao.DESPESA,
                LocalDate.now(), usuarioB, categoriaGlobal));
        transacaoBId = transacaoB.getId();
    }

    @Test
    void get_transacaoDeOutroUsuario_retorna404() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/{usuarioId}/transacoes/{id}", usuarioAId, transacaoBId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    void put_transacaoDeOutroUsuario_retorna404() throws Exception {
        TransacaoRequestDTO dto = new TransacaoRequestDTO(
                "Tentativa de alteração", new BigDecimal("1.00"), TipoTransacao.DESPESA, LocalDate.now(), categoriaId);

        mockMvc.perform(put("/api/v1/usuarios/{usuarioId}/transacoes/{id}", usuarioAId, transacaoBId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_transacaoDeOutroUsuario_retorna404() throws Exception {
        mockMvc.perform(delete("/api/v1/usuarios/{usuarioId}/transacoes/{id}", usuarioAId, transacaoBId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }
}
