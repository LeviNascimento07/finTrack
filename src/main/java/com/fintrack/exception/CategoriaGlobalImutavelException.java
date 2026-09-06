package com.fintrack.exception;

/**
 * Categoria global e visivel/legivel por todos, mas ninguem pode
 * editar ou excluir: 403 (o usuario sabe que ela existe, so nao pode mexer).
 */
public class CategoriaGlobalImutavelException extends RuntimeException {

    public CategoriaGlobalImutavelException() {
        super("Categoria global não pode ser alterada ou excluída");
    }
}
