package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.enums.FormatoRelatorio;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class RelatorioPresencasDTO {

    @NotNull(message = "Informe a turma.")
    private Integer idTurma;

    @NotNull(message = "Informe a data inicial.")
    private LocalDate dataInicio;

    @NotNull(message = "Informe a data final.")
    private LocalDate dataFim;

    @NotNull(message = "Informe o formato do arquivo.")
    private FormatoRelatorio formato;

    private boolean incluirOcorrencias = true;

    public Integer getIdTurma() {
        return idTurma;
    }

    public void setIdTurma(Integer idTurma) {
        this.idTurma = idTurma;
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

    public FormatoRelatorio getFormato() {
        return formato;
    }

    public void setFormato(FormatoRelatorio formato) {
        this.formato = formato;
    }

    public boolean isIncluirOcorrencias() {
        return incluirOcorrencias;
    }

    public void setIncluirOcorrencias(boolean incluirOcorrencias) {
        this.incluirOcorrencias = incluirOcorrencias;
    }
}
