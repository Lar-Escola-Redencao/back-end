package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.ContatoAssistido;
import br.org.larescolaredencao.model.enums.Parentesco;

public class VinculoContatoResponseDTO {

    private Integer idAssistido;
    private String nomeAssistido;
    private String imagemPerfil;
    private Parentesco parentesco;
    private Boolean principal;

    public VinculoContatoResponseDTO(ContatoAssistido ca) {
        this.idAssistido = ca.getAssistido().getId();
        this.nomeAssistido = ca.getAssistido().getNomeCompleto();
        this.imagemPerfil = ca.getAssistido().getImagemPerfil();
        this.parentesco = ca.getParentesco();
        this.principal = ca.getPrincipal();
    }

    public Integer getIdAssistido() {
        return idAssistido;
    }

    public String getNomeAssistido() {
        return nomeAssistido;
    }

    public String getImagemPerfil() {
        return imagemPerfil;
    }

    public Parentesco getParentesco() {
        return parentesco;
    }

    public Boolean getPrincipal() {
        return principal;
    }
}