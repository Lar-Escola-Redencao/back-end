package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record EntrevistaDTO(@NotNull LocalDateTime dataEntrevista,
                           @NotBlank @Size(max = 20000) String relato) { }
