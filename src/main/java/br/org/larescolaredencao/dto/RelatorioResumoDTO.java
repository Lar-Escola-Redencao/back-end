package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.enums.FormatoRelatorio;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public class RelatorioResumoDTO {

    @NotEmpty(message = "Selecione ao menos uma turma.")
    private List<@NotNull Integer> idsTurmas;

    @NotNull(message = "Informe a data inicial.")
    private LocalDate dataInicio;

    @NotNull(message = "Informe a data final.")
    private LocalDate dataFim;

    @NotNull(message = "Informe o formato do arquivo.")
    private FormatoRelatorio formato;

    public List<Integer> getIdsTurmas() {
        return idsTurmas;
    }

    public void setIdsTurmas(List<Integer> idsTurmas) {
        this.idsTurmas = idsTurmas;
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
}
