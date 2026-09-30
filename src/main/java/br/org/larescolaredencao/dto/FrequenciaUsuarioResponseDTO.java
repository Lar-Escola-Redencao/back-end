package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.Frequencia;
import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.Ocorrencia;

import java.util.List;
import java.util.stream.Collectors;

public class FrequenciaUsuarioResponseDTO {

    private Integer idMatricula;
    private Integer idUsuario;
    private String nomeUsuario;
    private String imagemPerfil;
    private Integer idFrequencia;
    private Boolean presente;
    private List<OcorrenciaResponseDTO> ocorrencias;

    public FrequenciaUsuarioResponseDTO(Matricula matricula, Frequencia frequencia, List<Ocorrencia> ocorrencias) {
        this.idMatricula = matricula.getId();
        this.idUsuario = matricula.getUsuario().getId();
        this.nomeUsuario = matricula.getUsuario().getNomeCompleto();
        this.imagemPerfil = matricula.getUsuario().getImagemPerfil();
        this.idFrequencia = frequencia != null ? frequencia.getId() : null;
        this.presente = frequencia != null ? frequencia.getPresente() : null;
        this.ocorrencias = ocorrencias != null ? ocorrencias.stream().map(OcorrenciaResponseDTO::new).collect(Collectors.toList()) : List.of();
    }

    public Integer getIdMatricula() {
        return idMatricula;
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

    public Integer getIdFrequencia() {
        return idFrequencia;
    }

    public Boolean getPresente() {
        return presente;
    }

    public List<OcorrenciaResponseDTO> getOcorrencias() {
        return ocorrencias;
    }
}