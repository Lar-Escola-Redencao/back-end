package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;
import java.nio.charset.StandardCharsets;

public class AlterarSenhaDTO {

    @NotBlank
    private String senhaAtual;

    @NotBlank
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{6,}$", message = "A senha deve conter no mínimo 6 caracteres, incluindo pelo menos uma letra maiúscula e um número.")
    private String novaSenha;

    @NotBlank
    private String confirmacaoSenha;

    @AssertTrue(message = "A nova senha deve ter no máximo 72 bytes.")
    public boolean isTamanhoNovaSenhaValido() {
        return novaSenha == null || novaSenha.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    public String getSenhaAtual() {
        return senhaAtual;
    }

    public void setSenhaAtual(String senhaAtual) {
        this.senhaAtual = senhaAtual;
    }

    public String getNovaSenha() {
        return novaSenha;
    }

    public void setNovaSenha(String novaSenha) {
        this.novaSenha = novaSenha;
    }

    public String getConfirmacaoSenha() {
        return confirmacaoSenha;
    }

    public void setConfirmacaoSenha(String confirmacaoSenha) {
        this.confirmacaoSenha = confirmacaoSenha;
    }
}
