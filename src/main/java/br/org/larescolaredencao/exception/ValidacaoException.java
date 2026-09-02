package br.org.larescolaredencao.exception;

import java.util.Map;

public class ValidacaoException extends RuntimeException {

    private final Map<String, String> erros;

    public ValidacaoException(Map<String, String> erros) {
        super("Erro de validação");
        this.erros = erros;
    }

    public Map<String, String> getErros() {
        return erros;
    }
}
