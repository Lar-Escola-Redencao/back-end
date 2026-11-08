package br.org.larescolaredencao.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ProtecaoAutenticacaoException extends ResponseStatusException {
    private final boolean captchaRequired;
    private final long retryAfterSeconds;

    public ProtecaoAutenticacaoException(HttpStatus status, String mensagem, boolean captchaRequired, long segundos) {
        super(status, mensagem);
        this.captchaRequired = captchaRequired;
        this.retryAfterSeconds = segundos;
    }

    public boolean isCaptchaRequired() { return captchaRequired; }
    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
