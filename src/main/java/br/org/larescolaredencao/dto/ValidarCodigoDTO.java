package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ValidarCodigoDTO {

    @NotBlank
    @Size(min = 6, max = 6)
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}