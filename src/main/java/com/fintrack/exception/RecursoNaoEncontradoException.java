package com.fintrack.exception;

/**
 * Uso generico quando o {usuarioId} da rota nao bate com o usuario
 * autenticado: 404 e nao 403, para nao confirmar se aquele id existe.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException() {
        super("Recurso não encontrado");
    }
}
