package br.org.larescolaredencao.dto;

import java.time.LocalTime;

import br.org.larescolaredencao.model.Turma;
import br.org.larescolaredencao.model.enums.Periodo;

public class TurmaResponseDTO {

    private Integer id;
    private Periodo periodo;
    private Integer unidadeId;
    private String unidadeNome;
    private LocalTime horaInicio;
    private LocalTime horaFim;

    public TurmaResponseDTO(Turma turma) {
        this.id = turma.getId();
        this.periodo = turma.getPeriodo();
        this.unidadeId = turma.getUnidade().getId();
        this.unidadeNome = turma.getUnidade().getNome();
        this.horaInicio = turma.getHoraInicio();
        this.horaFim = turma.getHoraFim();
    }

    public Integer getId() {
        return id;
    }

    public Periodo getPeriodo() {
        return periodo;
    }

    public Integer getUnidadeId() {
        return unidadeId;
    }

    public String getUnidadeNome() {
        return unidadeNome;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFim() {
        return horaFim;
    }
}
