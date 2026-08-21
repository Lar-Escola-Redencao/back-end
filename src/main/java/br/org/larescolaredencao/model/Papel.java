package br.org.larescolaredencao.model;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "papel")
public class Papel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
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
