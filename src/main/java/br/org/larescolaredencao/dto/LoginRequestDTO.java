package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(@NotBlank String identificador, @NotBlank String senha, Boolean lembrarMe) {}
