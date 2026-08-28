package br.org.larescolaredencao.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import br.org.larescolaredencao.model.enums.DiaSemana;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class CriarTurmaDTO {

    @NotBlank
    @Size(max = 150)
    private String nome;

    @NotNull
    private Integer unidadeId;

    @NotEmpty
    private List<DiaSemana> diasSemana;

    @NotNull
    private LocalTime horaInicio;

    @NotNull
    private LocalTime horaFim;

    @NotNull
    private LocalDate dataInicio;

    @NotNull
    private LocalDate dataFim;

    @NotNull
    @Positive
    private Integer vagas;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getUnidadeId() {
        return unidadeId;
    }

    public void setUnidadeId(Integer unidadeId) {
        this.unidadeId = unidadeId;
    }

    public List<DiaSemana> getDiasSemana() {
        return diasSemana;
    }

    public void setDiasSemana(List<DiaSemana> diasSemana) {
        this.diasSemana = diasSemana;
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

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public Integer getVagas() {
        return vagas;
    }

    public void setVagas(Integer vagas) {
        this.vagas = vagas;
    }
}
