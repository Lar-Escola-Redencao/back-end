package br.org.larescolaredencao.model;

import br.org.larescolaredencao.model.enums.Parentesco;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "contato_assistido")
public class ContatoAssistido {

    @EmbeddedId
    private ContatoAssistidoId id = new ContatoAssistidoId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idAssistido")
    @JoinColumn(name = "id_assistido")
    private Assistido assistido;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idContato")
    @JoinColumn(name = "id_contato")
    private Contato contato;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Parentesco parentesco;

    @Column(nullable = false)
    private Boolean principal = false;

    public ContatoAssistidoId getId() {
        return id;
    }

    public void setId(ContatoAssistidoId id) {
        this.id = id;
    }

    public Assistido getAssistido() {
        return assistido;
    }

    public void setAssistido(Assistido assistido) {
        this.assistido = assistido;
    }

    public Contato getContato() {
        return contato;
    }

    public void setContato(Contato contato) {
        this.contato = contato;
    }

    public Parentesco getParentesco() {
        return parentesco;
    }

    public void setParentesco(Parentesco parentesco) {
        this.parentesco = parentesco;
    }

    public Boolean getPrincipal() {
        return principal;
    }

    public void setPrincipal(Boolean principal) {
        this.principal = principal;
    }
}