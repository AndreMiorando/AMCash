package br.com.amcash.shared.exception;

public class CaptchaRequiredException extends RuntimeException {

    public CaptchaRequiredException() {
        super("Captcha obrigatório");
    }
}
