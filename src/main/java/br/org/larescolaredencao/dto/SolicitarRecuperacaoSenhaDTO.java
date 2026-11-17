package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SolicitarRecuperacaoSenhaDTO {

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @NotBlank(message = "Conclua a verificação de segurança.")
    @Size(max = 2048)
    private String captchaToken;

    public String getCaptchaToken() { return captchaToken; }
    public void setCaptchaToken(String captchaToken) { this.captchaToken = captchaToken; }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
