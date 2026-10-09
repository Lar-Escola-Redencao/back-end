package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class TransferirTurmaDTO {
    
    @NotNull(message = "A nova turma é obrigatória.")
    private Integer idTurmaNova;

    private LocalDate dataTransferencia;

    public Integer getIdTurmaNova() {
        return idTurmaNova;
    }

    public void setIdTurmaNova(Integer idTurmaNova) {
        this.idTurmaNova = idTurmaNova;
    }

    public LocalDate getDataTransferencia() {
        return dataTransferencia;
    }

    public void setDataTransferencia(LocalDate dataTransferencia) {
        this.dataTransferencia = dataTransferencia;
    }
}
