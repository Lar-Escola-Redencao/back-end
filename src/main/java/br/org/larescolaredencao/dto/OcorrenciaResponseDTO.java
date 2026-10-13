package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.Ocorrencia;
import br.org.larescolaredencao.model.enums.TipoOcorrencia;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class OcorrenciaResponseDTO {

    private Integer id;
    private Integer idMatricula;
    private LocalDate dataOcorrencia;
    private LocalDateTime dataCriacao;
    private String descricao;
    private TipoOcorrencia tipoOcorrencia;
    private String nomeMembro;

    public OcorrenciaResponseDTO(Ocorrencia ocorrencia) {
        this.id = ocorrencia.getId();
        this.idMatricula = ocorrencia.getMatricula().getId();
        this.dataOcorrencia = ocorrencia.getDataOcorrencia();
        this.dataCriacao = ocorrencia.getDataCriacao();
        this.descricao = ocorrencia.getDescricao();
        this.tipoOcorrencia = ocorrencia.getTipoOcorrencia();
        this.nomeMembro = ocorrencia.getMembro().getNomeCompleto();
    }

    public Integer getId() {
        return id;
    }

    public Integer getIdMatricula() {
        return idMatricula;
    }

    public LocalDate getDataOcorrencia() {
        return dataOcorrencia;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public String getDescricao() {
        return descricao;
    }

    public TipoOcorrencia getTipoOcorrencia() {
        return tipoOcorrencia;
    }

    public String getNomeMembro() {
        return nomeMembro;
    }
}