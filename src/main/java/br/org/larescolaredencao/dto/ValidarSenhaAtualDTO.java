package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.NotBlank;

public class ValidarSenhaAtualDTO {

    @NotBlank
    private String senhaAtual;

    public String getSenhaAtual() {
        return senhaAtual;
    }

    public void setSenhaAtual(String senhaAtual) {
        this.senhaAtual = senhaAtual;
    }
}
