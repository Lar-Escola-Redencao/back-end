package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class InativarUsuarioDTO {

    @NotNull(message = "A data de desligamento é obrigatória.")
    private LocalDate dataDesligamento;
    
    @NotBlank(message = "A justificativa do desligamento é obrigatória.")
    private String justificativa;

    public LocalDate getDataDesligamento() {
        return dataDesligamento;
    }

    public void setDataDesligamento(LocalDate dataDesligamento) {
        this.dataDesligamento = dataDesligamento;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }
}
