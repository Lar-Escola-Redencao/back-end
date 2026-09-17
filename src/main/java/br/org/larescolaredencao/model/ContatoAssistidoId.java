package br.org.larescolaredencao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ContatoAssistidoId implements Serializable {

    @Column(name = "id_assistido")
    private Integer idAssistido;

    @Column(name = "id_contato")
    private Integer idContato;

    public ContatoAssistidoId() {
    }

    public ContatoAssistidoId(Integer idAssistido, Integer idContato) {
        this.idAssistido = idAssistido;
        this.idContato = idContato;
    }

    public Integer getIdAssistido() {
        return idAssistido;
    }

    public void setIdAssistido(Integer idAssistido) {
        this.idAssistido = idAssistido;
    }

    public Integer getIdContato() {
        return idContato;
    }

    public void setIdContato(Integer idContato) {
        this.idContato = idContato;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContatoAssistidoId that = (ContatoAssistidoId) o;
        return Objects.equals(idAssistido, that.idAssistido) && Objects.equals(idContato, that.idContato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idAssistido, idContato);
    }
}