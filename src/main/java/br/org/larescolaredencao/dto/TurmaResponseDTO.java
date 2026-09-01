package br.org.larescolaredencao.dto;

import java.time.LocalTime;

import br.org.larescolaredencao.model.Turma;
import br.org.larescolaredencao.model.enums.Periodo;

public class TurmaResponseDTO {

    private Integer id;
    private Integer unidadeId;
    private String unidadeNome;
    private Periodo periodo;
    private LocalTime horaInicio;
    private LocalTime horaFim;

    public TurmaResponseDTO(Turma turma) {
        this.id = turma.getId();
        this.unidadeId = turma.getUnidade().getId();
        this.unidadeNome = turma.getUnidade().getNome();
        this.periodo = turma.getPeriodo();
        this.horaInicio = turma.getHoraInicio();
        this.horaFim = turma.getHoraFim();
    }

    public Integer getId() {
        return id;
    }

    public Integer getUnidadeId() {
        return unidadeId;
    }

    public String getUnidadeNome() {
        return unidadeNome;
    }

    public Periodo getPeriodo() {
        return periodo;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFim() {
        return horaFim;
    }
}
