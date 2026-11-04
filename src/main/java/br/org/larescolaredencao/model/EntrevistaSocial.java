package br.org.larescolaredencao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@org.hibernate.envers.Audited
@Table(name = "entrevista_social")
public class EntrevistaSocial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "id_membro", nullable = false)
    private Integer idMembro;

    @Column(name = "data_entrevista", nullable = false)
    private LocalDateTime dataEntrevista;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String relato;

    @Column(name = "data_registro")
    private LocalDateTime dataRegistro = LocalDateTime.now();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Integer getIdMembro() {
        return idMembro;
    }

    public void setIdMembro(Integer idMembro) {
        this.idMembro = idMembro;
    }

    public LocalDateTime getDataEntrevista() {
        return dataEntrevista;
    }

    public void setDataEntrevista(LocalDateTime dataEntrevista) {
        this.dataEntrevista = dataEntrevista;
    }

    public String getRelato() {
        return relato;
    }

    public void setRelato(String relato) {
        this.relato = relato;
    }

    public LocalDateTime getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(LocalDateTime dataRegistro) {
        this.dataRegistro = dataRegistro;
    }
}
