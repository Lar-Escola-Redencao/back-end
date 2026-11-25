package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.NotBlank;
import br.org.larescolaredencao.validacao.EmailOuCpf;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(@NotBlank @EmailOuCpf @Size(max = 254) String identificador, @NotBlank String senha,
        Boolean lembrarMe, @Size(max = 2048) String captchaToken) {}
