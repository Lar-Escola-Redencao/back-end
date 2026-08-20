package br.org.larescolaredencao.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.org.larescolaredencao.model.Parceiro;
import br.org.larescolaredencao.model.Evento;
import br.org.larescolaredencao.model.enums.TipoEvento;

public class EventoResponseDTO {

    private Integer id;
    private String titulo;
    private String descricao;
    private LocalDateTime dataEvento;
    private String endereco;
    private String imagem;
    private BigDecimal valor;
    private TipoEvento tipoEvento;
    private String comentarioPosEvento;
    private List<Parceiro> parceiros;

    public EventoResponseDTO(Evento evento) {
        this.id = evento.getId();
        this.titulo = evento.getTitulo();
        this.descricao = evento.getDescricao();
        this.dataEvento = evento.getDataEvento();
        this.endereco = evento.getEndereco();
        this.imagem = evento.getImagem();
        this.valor = evento.getValor();
        this.tipoEvento = evento.getTipoEvento();
        this.comentarioPosEvento = evento.getComentarioPosEvento();
        this.parceiros = evento.getParceiros();
    }

    public Integer getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public LocalDateTime getDataEvento() {
        return dataEvento;
    }

    public String getEndereco() {
        return endereco;
    }

    public String getImagem() {
        return imagem;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public TipoEvento getTipoEvento() {
        return tipoEvento;
    }

    public String getComentarioPosEvento() {
        return comentarioPosEvento;
    }
    
    public List<Parceiro> getParceiros() {
        return parceiros;
    }
}