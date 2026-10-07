package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.ContatoUsuario;
import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.enums.Parentesco;
import br.org.larescolaredencao.model.enums.StatusMatricula;
import java.time.LocalDate;

public class VinculoContatoResponseDTO {

    private Integer idUsuario;
    private String nomeUsuario;
    private String imagemPerfil;
    private Parentesco parentesco;
    private Boolean principal;
    private Integer idUnidade;
    private String nomeUnidade;
    private StatusMatricula statusMatricula;
    private LocalDate dataDesligamento;

    public VinculoContatoResponseDTO(ContatoUsuario ca, Matricula matriculaAtiva) {
        this.idUsuario = ca.getUsuario().getId();
        this.nomeUsuario = ca.getUsuario().getNomeCompleto();
        this.imagemPerfil = ca.getUsuario().getImagemPerfil();
        this.parentesco = ca.getParentesco();
        this.principal = ca.getPrincipal();

        if (matriculaAtiva != null) {
            this.idUnidade = matriculaAtiva.getTurma().getUnidade().getId();
            this.nomeUnidade = matriculaAtiva.getTurma().getUnidade().getNome();
            this.statusMatricula = matriculaAtiva.getStatus();
            this.dataDesligamento = matriculaAtiva.getDataDesligamento();
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

    public StatusMatricula getStatusMatricula() {
        return statusMatricula;
    }

    public LocalDate getDataDesligamento() {
        return dataDesligamento;
    }
}
