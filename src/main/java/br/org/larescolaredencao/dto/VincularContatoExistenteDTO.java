package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.enums.Parentesco;
import jakarta.validation.constraints.NotNull;

public class VincularContatoExistenteDTO {

    @NotNull
    private Parentesco parentesco;

    @NotNull
    private Boolean principal;

    public Parentesco getParentesco() {
        return parentesco;
    }

    public void setParentesco(Parentesco parentesco) {
        this.parentesco = parentesco;
    }

    public Boolean getPrincipal() {
        return principal;
    }

    public void setPrincipal(Boolean principal) {
        this.principal = principal;
    }
}