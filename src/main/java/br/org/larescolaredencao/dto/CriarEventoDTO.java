package br.org.larescolaredencao.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import br.org.larescolaredencao.model.enums.TipoEvento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CriarEventoDTO {
	
	@NotBlank
	@Size(max = 150)
    private String titulo;
	
	@NotBlank
    private String descricao;
	
	@NotNull
    private LocalDateTime dataEvento;
	
	@NotBlank
    private String endereco;
    private MultipartFile imagem;
    private BigDecimal valor;
    
    @NotNull
    private TipoEvento tipoEvento;
    private List<Long> parceirosIds;

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDateTime getDataEvento() {
        return dataEvento;
    }

    public void setDataEvento(LocalDateTime dataEvento) {
        this.dataEvento = dataEvento;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public MultipartFile getImagem() {
        return imagem;
    }

    public void setImagem(MultipartFile imagem) {
        this.imagem = imagem;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public TipoEvento getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(TipoEvento tipoEvento) {
        this.tipoEvento = tipoEvento;
    }
    
    public List<Long> getParceirosIds() {
        return parceirosIds;
    }

    public void setParceirosIds(List<Long> parceirosIds) {
        this.parceirosIds = parceirosIds;
    }
}