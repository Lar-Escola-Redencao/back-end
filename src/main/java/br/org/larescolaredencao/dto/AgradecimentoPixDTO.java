package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgradecimentoPixDTO(
        @NotBlank(message = "Informe o e-mail.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 254, message = "O e-mail deve ter até 254 caracteres.") String email,
        @NotBlank(message = "Faça a verificação de segurança.")
        @Size(max = 2048, message = "A verificação de segurança não é válida.") String captchaToken) {
}
