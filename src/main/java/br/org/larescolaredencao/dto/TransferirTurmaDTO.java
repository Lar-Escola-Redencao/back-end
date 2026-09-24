package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.NotNull;

public class TransferirTurmaDTO {
    
    @NotNull(message = "A nova turma é obrigatória.")
    private Integer idTurmaNova;

    public Integer getIdTurmaNova() {
        return idTurmaNova;
    }

    public void setIdTurmaNova(Integer idTurmaNova) {
        this.idTurmaNova = idTurmaNova;
    }
}