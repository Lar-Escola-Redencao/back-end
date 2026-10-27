package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record LeituraHistoricoDTO(@PositiveOrZero long ultimoEvento) {
}
