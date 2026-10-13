package br.org.larescolaredencao.model;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

// Link de um evento em uma rede social. Não é um @ManyToMany simples porque a ligação tem dado próprio (url_link).
@Entity
@Table(name = "evento_rede_social")
public class EventoRedeSocial {

    @EmbeddedId
    private EventoRedeSocialId id = new EventoRedeSocialId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idEvento")
    @JoinColumn(name = "id_evento")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idRedeSocial")
    @JoinColumn(name = "id_rede_social")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private RedeSocial redeSocial;

    @Column(name = "url_link", nullable = false)
    private String urlLink;

    public EventoRedeSocialId getId() {
        return id;
    }

    public void setId(EventoRedeSocialId id) {
        this.id = id;
    }

    public Evento getEvento() {
        return evento;
    }

    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    public RedeSocial getRedeSocial() {
        return redeSocial;
    }

    public void setRedeSocial(RedeSocial redeSocial) {
        this.redeSocial = redeSocial;
    }

    public String getUrlLink() {
        return urlLink;
    }

    public void setUrlLink(String urlLink) {
        this.urlLink = urlLink;
    }
}
