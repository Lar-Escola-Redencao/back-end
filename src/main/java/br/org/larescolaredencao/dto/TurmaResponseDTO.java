package br.org.larescolaredencao.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import br.org.larescolaredencao.model.Turma;
import br.org.larescolaredencao.model.enums.DiaSemana;

public class TurmaResponseDTO {

    private Integer id;
    private String nome;
    private Integer unidadeId;
    private String unidadeNome;
    private List<DiaSemana> diasSemana;
    private LocalTime horaInicio;
    private LocalTime horaFim;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private Integer vagas;

    public TurmaResponseDTO(Turma turma) {
        this.id = turma.getId();
        this.nome = turma.getNome();
        this.unidadeId = turma.getUnidade().getId();
        this.unidadeNome = turma.getUnidade().getNome();
        this.diasSemana = turma.getDiasSemana();
        this.horaInicio = turma.getHoraInicio();
        this.horaFim = turma.getHoraFim();
        this.dataInicio = turma.getDataInicio();
        this.dataFim = turma.getDataFim();
        this.vagas = turma.getVagas();
    }

    public Integer getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Integer getUnidadeId() {
        return unidadeId;
    }

    public String getUnidadeNome() {
        return unidadeNome;
    }

    public List<DiaSemana> getDiasSemana() {
        return diasSemana;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFim() {
        return horaFim;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public Integer getVagas() {
        return vagas;
    }
}
