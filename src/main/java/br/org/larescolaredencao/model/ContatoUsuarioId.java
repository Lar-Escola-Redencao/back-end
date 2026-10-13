package br.org.larescolaredencao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ContatoUsuarioId implements Serializable {

	private static final long serialVersionUID = 1L;

	@Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "id_contato")
    private Integer idContato;

    public ContatoUsuarioId() {
    }

    public ContatoUsuarioId(Integer idUsuario, Integer idContato) {
        this.idUsuario = idUsuario;
        this.idContato = idContato;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
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
        ContatoUsuarioId that = (ContatoUsuarioId) o;
        return Objects.equals(idUsuario, that.idUsuario) && Objects.equals(idContato, that.idContato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUsuario, idContato);
    }
}