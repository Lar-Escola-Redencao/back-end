package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;

public class AtualizarFrequenciaDTO {

    @NotNull
    private Boolean presente;

    @NotNull(message = "Confirme o preenchimento das frequências.")
    @AssertTrue(message = "Confirme o preenchimento das frequências.")
    private Boolean declaracaoAceita;

    public Boolean getDeclaracaoAceita() {
        return declaracaoAceita;
    }

    public void setDeclaracaoAceita(Boolean declaracaoAceita) {
        this.declaracaoAceita = declaracaoAceita;
    }

    public Boolean getPresente() {
        return presente;
    }

    public void setPresente(Boolean presente) {
        this.presente = presente;
    }
}
