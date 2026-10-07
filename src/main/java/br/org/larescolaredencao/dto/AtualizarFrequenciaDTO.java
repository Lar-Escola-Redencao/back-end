package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.NotNull;

public class AtualizarFrequenciaDTO {

    @NotNull
    private Boolean presente;

    public Boolean getPresente() {
        return presente;
    }

    public void setPresente(Boolean presente) {
        this.presente = presente;
    }
}