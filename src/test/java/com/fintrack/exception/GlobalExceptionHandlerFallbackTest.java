package com.fintrack.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * O fallback e o caminho onde vazamento custa mais caro (qualquer excecao
 * nao mapeada passa por ele) e nao era exercitado por nenhum teste desde a
 * etapa 5. Chama handleFallback diretamente — nao precisa de contexto
 * Spring, so garantir que uma mensagem de excecao sensivel nunca escapa
 * para o corpo da resposta.
 */
class GlobalExceptionHandlerFallbackTest {

    @Test
    void handleFallback_naoVazaMensagemInternaDaExcecao() {
        String segredo = "detalhe interno secreto";
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/qualquer");
        when(request.getMethod()).thenReturn("GET");

        RuntimeException excecaoComDetalheSensivel = new RuntimeException(segredo);

        ResponseEntity<ErroResponse> resposta = handler.handleFallback(excecaoComDetalheSensivel, request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        ErroResponse corpo = resposta.getBody();
        assertThat(corpo).isNotNull();
        assertThat(corpo.toString()).doesNotContain(segredo);
        assertThat(corpo.mensagem()).doesNotContain(segredo);
        assertThat(corpo.erro()).doesNotContain(segredo);
        assertThat(corpo.mensagem()).isEqualTo("Erro interno do servidor");
    }
}
