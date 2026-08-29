package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RedefinirSenhaDTO {

    @NotBlank
    @Size(min = 6, max = 6)
    private String token;

    @NotBlank
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#]).{7,}$", message = "A senha deve conter mais de 6 caracteres, incluindo pelo menos uma letra maiúscula, um número e um caractere especial.")
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