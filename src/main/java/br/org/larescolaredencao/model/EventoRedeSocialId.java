package br.org.larescolaredencao.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class EventoRedeSocialId implements Serializable {

    @Column(name = "id_evento")
    private Integer idEvento;

    @Column(name = "id_rede_social")
    private Long idRedeSocial;

    public EventoRedeSocialId() {
    }

    public EventoRedeSocialId(Integer idEvento, Long idRedeSocial) {
        this.idEvento = idEvento;
        this.idRedeSocial = idRedeSocial;
    }

    public Integer getIdEvento() {
        return idEvento;
    }

    public Long getIdRedeSocial() {
        return idRedeSocial;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EventoRedeSocialId outro)) {
            return false;
        }
        return Objects.equals(idEvento, outro.idEvento) && Objects.equals(idRedeSocial, outro.idRedeSocial);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idEvento, idRedeSocial);
    }
}
