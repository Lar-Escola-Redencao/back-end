package br.org.larescolaredencao.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public class SalvarFrequenciaEmLoteDTO {

    @NotNull
    private Integer idTurma;

    @NotNull
    private LocalDate data;

    @NotNull(message = "Confirme o preenchimento das frequências.")
    @AssertTrue(message = "Confirme o preenchimento das frequências.")
    private Boolean declaracaoAceita;

    public Boolean getDeclaracaoAceita() {
        return declaracaoAceita;
    }

    public void setDeclaracaoAceita(Boolean declaracaoAceita) {
        this.declaracaoAceita = declaracaoAceita;
    }

    @NotEmpty
    @Valid
    private List<@NotNull FrequenciaDTO> frequencias;

    public Integer getIdTurma() {
        return idTurma;
    }

    public void setIdTurma(Integer idTurma) {
        this.idTurma = idTurma;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public List<FrequenciaDTO> getFrequencias() {
        return frequencias;
    }

    public void setFrequencias(List<FrequenciaDTO> frequencias) {
        this.frequencias = frequencias;
    }
}
