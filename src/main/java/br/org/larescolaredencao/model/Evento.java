package br.org.larescolaredencao.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.org.larescolaredencao.model.enums.TipoEvento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import br.org.larescolaredencao.model.Parceiro;

@Entity
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Size(max = 150)
    private String titulo;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String descricao;

    @NotNull
    private LocalDateTime dataEvento;

    @NotBlank
    private String endereco;

    private String imagem;

    private BigDecimal valor = BigDecimal.ZERO;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TipoEvento tipoEvento;

    @Column(columnDefinition = "TEXT")
    private String comentarioPosEvento;
    
    @ManyToMany
    @JoinTable(
        name = "parceiro_evento",
        joinColumns = @JoinColumn(name = "id_evento"),
        inverseJoinColumns = @JoinColumn(name = "id_parceiro")
    )
    private List<Parceiro> parceiros = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDateTime getDataEvento() {
        return dataEvento;
    }

    public void setDataEvento(LocalDateTime dataEvento) {
        this.dataEvento = dataEvento;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getImagem() {
        return imagem;
    }

    public void setImagem(String imagem) {
        this.imagem = imagem;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public TipoEvento getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(TipoEvento tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getComentarioPosEvento() {
        return comentarioPosEvento;
    }

    public void setComentarioPosEvento(String comentarioPosEvento) {
        this.comentarioPosEvento = comentarioPosEvento;
    }
    
    public List<Parceiro> getParceiros() {
        return parceiros;
    }

    public void setParceiros(List<Parceiro> parceiros) {
        this.parceiros = parceiros;
    }
}