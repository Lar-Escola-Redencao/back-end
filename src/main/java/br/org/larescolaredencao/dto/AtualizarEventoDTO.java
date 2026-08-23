package br.org.larescolaredencao.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import br.org.larescolaredencao.model.enums.TipoEvento;
import jakarta.validation.constraints.Size;

public class AtualizarEventoDTO {
	
	@Size(max = 150, message = "O título não pode ter mais de 150 caracteres")
    private String titulo;
    private String descricao;
    private LocalDateTime dataEvento;
    private String endereco;
    private MultipartFile imagem;
    private BigDecimal valor;
    private TipoEvento tipoEvento;
    private String comentarioPosEvento;
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

    public String getComentarioPosEvento() {
        return comentarioPosEvento;
    }

    public void setComentarioPosEvento(String comentarioPosEvento) {
        this.comentarioPosEvento = comentarioPosEvento;
    }
    
    public List<Long> getParceirosIds() {
        return parceirosIds;
    }

    public void setParceirosIds(List<Long> parceirosIds) {
        this.parceirosIds = parceirosIds;
    }
}