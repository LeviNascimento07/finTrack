package com.fintrack.exception;

/**
 * Mensagem deliberadamente generica: nao revela se o e-mail existe ou se
 * foi a senha que errou.
 */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("Credenciais inválidas");
    }
}
