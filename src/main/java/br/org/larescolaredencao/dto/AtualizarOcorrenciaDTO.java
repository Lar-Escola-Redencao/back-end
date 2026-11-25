package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.enums.TipoOcorrencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public class AtualizarOcorrenciaDTO {

    @Positive
    private Integer idMatricula;

    private LocalDateTime dataOcorrencia;

    public Integer getIdMatricula() { return idMatricula; }

    public void setIdMatricula(Integer idMatricula) { this.idMatricula = idMatricula; }

    @NotBlank
    private String descricao;

    @NotNull
    private TipoOcorrencia tipoOcorrencia;

    public String getDescricao() {
        return descricao;
    }

    public LocalDateTime getDataOcorrencia() {
        return dataOcorrencia;
    }

    public void setDataOcorrencia(LocalDateTime dataOcorrencia) {
        this.dataOcorrencia = dataOcorrencia;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public TipoOcorrencia getTipoOcorrencia() {
        return tipoOcorrencia;
    }

    public void setTipoOcorrencia(TipoOcorrencia tipoOcorrencia) {
        this.tipoOcorrencia = tipoOcorrencia;
    }
}
