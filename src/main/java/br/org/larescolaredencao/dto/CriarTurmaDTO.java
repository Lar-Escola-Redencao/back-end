package br.org.larescolaredencao.dto;

import java.time.LocalTime;

import br.org.larescolaredencao.model.enums.Periodo;
import jakarta.validation.constraints.NotNull;

public class CriarTurmaDTO {

    @NotNull
    private Periodo periodo;

    @NotNull
    private Integer unidadeId;

    @NotNull
    private LocalTime horaInicio;

    @NotNull
    private LocalTime horaFim;

    public Periodo getPeriodo() {
        return periodo;
    }

    public void setPeriodo(Periodo periodo) {
        this.periodo = periodo;
    }

    public Integer getUnidadeId() {
        return unidadeId;
    }

    public void setUnidadeId(Integer unidadeId) {
        this.unidadeId = unidadeId;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFim() {
        return horaFim;
    }

    public void setHoraFim(LocalTime horaFim) {
        this.horaFim = horaFim;
    }
}
