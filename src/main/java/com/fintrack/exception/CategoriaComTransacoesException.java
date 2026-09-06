package com.fintrack.exception;

public class CategoriaComTransacoesException extends RuntimeException {

    public CategoriaComTransacoesException() {
        super("Categoria possui transações associadas e não pode ser excluída");
    }
}
