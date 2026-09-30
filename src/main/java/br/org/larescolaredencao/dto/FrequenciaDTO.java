package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.NotNull;

public class FrequenciaDTO {

    @NotNull
    private Integer idMatricula;

    @NotNull
    private Boolean presente;

    public Integer getIdMatricula() {
        return idMatricula;
    }

    public void setIdMatricula(Integer idMatricula) {
        this.idMatricula = idMatricula;
    }

    public Boolean getPresente() {
        return presente;
    }

    public void setPresente(Boolean presente) {
        this.presente = presente;
    }
}