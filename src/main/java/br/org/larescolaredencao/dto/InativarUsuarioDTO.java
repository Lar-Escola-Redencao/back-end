package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class InativarAssistidoDTO {

    @NotNull(message = "A data de desligamento é obrigatória.")
    private LocalDate dataDesligamento;

    public LocalDate getDataDesligamento() {
        return dataDesligamento;
    }

    public void setDataDesligamento(LocalDate dataDesligamento) {
        this.dataDesligamento = dataDesligamento;
    }
}