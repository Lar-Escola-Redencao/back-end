package br.org.larescolaredencao.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Bloco pai do conceito de blocos e componentes (ex.: "Estatuto Social e Atas",
 * "Relatórios Financeiros"). Agrupa os documentos filhos exibidos na página.
 */
@Entity
@Table(name = "secao")
public class Secao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 150)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String conteudo;

    private String imagem;

    private Boolean ativo;

    @ManyToOne
    @JoinColumn(name = "id_pagina", nullable = false)
    @JsonBackReference
    private Pagina pagina;

    @OneToMany(mappedBy = "secao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id DESC")
    @JsonManagedReference
    private List<Documento> documentos = new ArrayList<>();

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getTitulo() {
        return titulo;
    }
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }
    public String getConteudo() {
        return conteudo;
    }
    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }
    public String getImagem() {
        return imagem;
    }
    public void setImagem(String imagem) {
        this.imagem = imagem;
    }
    public Boolean getAtivo() {
        return ativo;
    }
    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
    public Pagina getPagina() {
        return pagina;
    }
    public void setPagina(Pagina pagina) {
        this.pagina = pagina;
    }
    public List<Documento> getDocumentos() {
        return documentos;
    }
    public void setDocumentos(List<Documento> documentos) {
        this.documentos = documentos;
    }
}
