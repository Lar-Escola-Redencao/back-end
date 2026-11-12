package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.NotBlank;
import br.org.larescolaredencao.validacao.EmailOuCpf;

public record LoginRequestDTO(@NotBlank @EmailOuCpf String identificador, @NotBlank String senha, Boolean lembrarMe) {}
