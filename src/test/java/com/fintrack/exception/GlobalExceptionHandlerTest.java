package com.fintrack.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.model.Usuario;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prova que 400, 404 e 409 (tres caminhos de excecao bem diferentes: erro de
 * validacao de DTO, {usuarioId} de outra pessoa, e e-mail duplicado no
 * registro) devolvem o MESMO formato de corpo — objetivo central da etapa 5.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String email;

    @BeforeEach
    void setUp() {
        email = "existente-" + UUID.randomUUID() + "@teste.com";
        usuarioRepository.save(new Usuario("Usuário Existente", email, passwordEncoder.encode("senha123")));
    }

    @Test
    void erro400DeValidacao_e400_e409_tem_o_mesmo_formato_de_corpo() throws Exception {
        // 400: email invalido no registro (MethodArgumentNotValidException)
        MvcResult resultado400 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"X\",\"email\":\"invalido\",\"senha\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // 404: {usuarioId} da rota nao bate com o autenticado (RecursoNaoEncontradoException)
        String token = jwtService.gerarToken(email);
        MvcResult resultado404 = mockMvc.perform(get("/api/v1/usuarios/999999/categorias")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andReturn();

        // 409: e-mail ja cadastrado no registro (EmailJaCadastradoException)
        MvcResult resultado409 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Outro\",\"email\":\"" + email + "\",\"senha\":\"senha123\"}"))
                .andExpect(status().isConflict())
                .andReturn();

        for (MvcResult resultado : new MvcResult[]{resultado400, resultado404, resultado409}) {
            var corpo = objectMapper.readTree(resultado.getResponse().getContentAsString());

            assertThat(corpo.has("timestamp")).isTrue();
            assertThat(corpo.has("status")).isTrue();
            assertThat(corpo.has("erro")).isTrue();
            assertThat(corpo.has("mensagem")).isTrue();
            assertThat(corpo.has("path")).isTrue();
            assertThat(corpo.get("status").asInt()).isEqualTo(resultado.getResponse().getStatus());
        }
    }
}
