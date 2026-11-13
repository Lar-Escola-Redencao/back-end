package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.NotBlank;

/** Dados revisados do colaborador e a nova senha, enviados juntos para reativar o acesso. */
public class ReativarMembroDTO extends AtualizarMembroDTO {

    @NotBlank
    private String senha;

    private String confirmacaoSenha;

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getConfirmacaoSenha() {
        return confirmacaoSenha;
    }

    public void setConfirmacaoSenha(String confirmacaoSenha) {
        this.confirmacaoSenha = confirmacaoSenha;
    }
}
