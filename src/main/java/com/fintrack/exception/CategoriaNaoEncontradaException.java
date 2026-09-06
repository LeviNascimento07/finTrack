package com.fintrack.exception;

/**
 * Cobre tanto "id nao existe" quanto "id existe mas e de outro usuario":
 * a resposta e sempre 404, nunca 403, para nao confirmar a existencia do
 * recurso a quem nao e o dono.
 */
public class CategoriaNaoEncontradaException extends RuntimeException {

    public CategoriaNaoEncontradaException(Long id) {
        super("Categoria não encontrada: " + id);
    }
}
