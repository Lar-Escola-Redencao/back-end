package br.org.larescolaredencao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "papel")
public class Papel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Column(name = "nome_papel", nullable = false, unique = true, length = 50)
    private String nomePapel;

    @Column(length = 255)
    private String descricao;

    public Papel() {
    }

    public Papel(Integer id, String nomePapel, String descricao) {
        this.id = id;
        this.nomePapel = nomePapel;
        this.descricao = descricao;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNomePapel() {
        return nomePapel;
    }

    public void setNomePapel(String nomePapel) {
        this.nomePapel = nomePapel;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}