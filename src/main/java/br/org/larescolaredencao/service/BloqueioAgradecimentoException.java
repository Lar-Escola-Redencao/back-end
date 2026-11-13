package br.org.larescolaredencao.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class BloqueioAgradecimentoException extends ResponseStatusException {
    private final long retryAfterSeconds;

    public BloqueioAgradecimentoException(HttpStatus status, String mensagem, long retryAfterSeconds) {
        super(status, mensagem);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
