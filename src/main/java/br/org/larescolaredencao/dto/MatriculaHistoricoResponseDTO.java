package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.enums.StatusMatricula;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class MatriculaHistoricoResponseDTO {

    private final Integer id;
    private final Integer idUnidade;
    private final String nomeUnidade;
    private final Integer idTurma;
    private final String periodoTurma;
    private final String horaInicio;
    private final String horaFim;
    private final StatusMatricula status;
    private final LocalDateTime dataIngresso;
    private final LocalDate dataDesligamento;
    private final String justificativaEgresso;

    public MatriculaHistoricoResponseDTO(Matricula matricula) {
        this.id = matricula.getId();
        this.idUnidade = matricula.getTurma().getUnidade().getId();
        this.nomeUnidade = matricula.getTurma().getUnidade().getNome();
        this.idTurma = matricula.getTurma().getId();
        this.periodoTurma = matricula.getTurma().getPeriodo().name();
        this.horaInicio = matricula.getTurma().getHoraInicio().toString();
        this.horaFim = matricula.getTurma().getHoraFim().toString();
        this.status = matricula.getStatus();
        this.dataIngresso = matricula.getDataIngresso();
        this.dataDesligamento = matricula.getDataDesligamento();
        this.justificativaEgresso = matricula.getJustificativaEgresso();
    }

    public Integer getId() { return id; }
    public Integer getIdUnidade() { return idUnidade; }
    public String getNomeUnidade() { return nomeUnidade; }
    public Integer getIdTurma() { return idTurma; }
    public String getPeriodoTurma() { return periodoTurma; }
    public String getHoraInicio() { return horaInicio; }
    public String getHoraFim() { return horaFim; }
    public StatusMatricula getStatus() { return status; }
    public LocalDateTime getDataIngresso() { return dataIngresso; }
    public LocalDate getDataDesligamento() { return dataDesligamento; }
    public String getJustificativaEgresso() { return justificativaEgresso; }
}
