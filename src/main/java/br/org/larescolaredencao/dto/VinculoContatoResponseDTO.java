package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.ContatoUsuario;
import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.enums.Parentesco;

public class VinculoContatoResponseDTO {

    private Integer idUsuario;
    private String nomeUsuario;
    private String imagemPerfil;
    private Parentesco parentesco;
    private Boolean principal;
    private Integer idUnidade;
    private String nomeUnidade;

    public VinculoContatoResponseDTO(ContatoUsuario ca, Matricula matriculaAtiva) {
        this.idUsuario = ca.getUsuario().getId();
        this.nomeUsuario = ca.getUsuario().getNomeCompleto();
        this.imagemPerfil = ca.getUsuario().getImagemPerfil();
        this.parentesco = ca.getParentesco();
        this.principal = ca.getPrincipal();

        if (matriculaAtiva != null) {
            this.idUnidade = matriculaAtiva.getTurma().getUnidade().getId();
            this.nomeUnidade = matriculaAtiva.getTurma().getUnidade().getNome();
        }
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public String getNomeUsuario() {
        return nomeUsuario;
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

    public Integer getIdUnidade() {
        return idUnidade;
    }

    public String getNomeUnidade() {
        return nomeUnidade;
    }
}