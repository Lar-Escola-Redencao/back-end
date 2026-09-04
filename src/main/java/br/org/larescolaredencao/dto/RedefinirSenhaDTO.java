package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RedefinirSenhaDTO {

    @NotBlank
    @Size(min = 6, max = 6)
    private String token;

    @NotBlank
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{6,}$", message = "A senha deve conter no mínimo 6 caracteres, incluindo pelo menos uma letra maiúscula e um número.")
    private String novaSenha;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNovaSenha() {
        return novaSenha;
    }

    public void setNovaSenha(String novaSenha) {
        this.novaSenha = novaSenha;
    }
}