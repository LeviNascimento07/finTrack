package com.fintrack.exception;

/**
 * Cobre "id nao existe" e "id existe mas e de outro usuario": sempre 404.
 */
public class TransacaoNaoEncontradaException extends RuntimeException {

    public TransacaoNaoEncontradaException(Long id) {
        super("Transação não encontrada: " + id);
    }
}
