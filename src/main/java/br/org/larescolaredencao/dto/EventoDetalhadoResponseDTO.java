package br.org.larescolaredencao.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.org.larescolaredencao.model.Evento;
import br.org.larescolaredencao.model.MidiaEvento;
import br.org.larescolaredencao.model.enums.TipoEvento;

public class EventoDetalhadoResponseDTO {

    private Integer id;
    private String titulo;
    private String descricao;
    private LocalDateTime dataEvento;
    private String endereco;
    private String imagem;
    private BigDecimal valor;
    private TipoEvento tipoEvento;
    private String comentarioPosEvento;
    private List<MidiaEventoResponseDTO> midiaEvento;
    private List<ParceiroResponseDTO> parceiros;

    public EventoDetalhadoResponseDTO(Evento evento, List<MidiaEvento> midias) {
        this.id = evento.getId();
        this.titulo = evento.getTitulo();
        this.descricao = evento.getDescricao();
        this.dataEvento = evento.getDataEvento();
        this.endereco = evento.getEndereco();
        this.imagem = evento.getImagem();
        this.valor = evento.getValor();
        this.tipoEvento = evento.getTipoEvento();
        this.comentarioPosEvento = evento.getComentarioPosEvento();
        this.midiaEvento = midias == null ? List.of()
                : midias.stream().map(MidiaEventoResponseDTO::new).toList();
        this.parceiros = evento.getParceiros() == null ? List.of()
                : evento.getParceiros().stream().map(ParceiroResponseDTO::new).toList();
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

    public List<MidiaEventoResponseDTO> getMidiaEvento() {
        return midiaEvento;
    }

    public List<ParceiroResponseDTO> getParceiros() {
        return parceiros;
    }
}
