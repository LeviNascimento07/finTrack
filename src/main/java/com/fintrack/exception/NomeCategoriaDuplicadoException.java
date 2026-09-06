package com.fintrack.exception;

public class NomeCategoriaDuplicadoException extends RuntimeException {

    public NomeCategoriaDuplicadoException(String nome) {
        super("Já existe uma categoria com o nome: " + nome);
    }
}
