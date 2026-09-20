package br.com.amcash.shared.exception;

public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException() {
        super("Muitas tentativas. Tente novamente em alguns minutos.");
    }
}
