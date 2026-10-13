package br.org.larescolaredencao.model;

import br.org.larescolaredencao.model.enums.EscolaridadeFamiliar;
import br.org.larescolaredencao.model.enums.Parentesco;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "composicao_familiar")
public class ComposicaoFamiliar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "nome_completo", nullable = false, length = 150)
    private String nomeCompleto;

    @Enumerated(EnumType.STRING)
    @Column(name = "parentesco_vinculo", nullable = false)
    private Parentesco parentescoVinculo;

    private Integer idade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EscolaridadeFamiliar escolaridade;

    private BigDecimal renda = BigDecimal.ZERO;

    private BigDecimal beneficios = BigDecimal.ZERO;

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

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public Parentesco getParentescoVinculo() {
        return parentescoVinculo;
    }

    public void setParentescoVinculo(Parentesco parentescoVinculo) {
        this.parentescoVinculo = parentescoVinculo;
    }

    public Integer getIdade() {
        return idade;
    }

    public void setIdade(Integer idade) {
        this.idade = idade;
    }

    public EscolaridadeFamiliar getEscolaridade() {
        return escolaridade;
    }

    public void setEscolaridade(EscolaridadeFamiliar escolaridade) {
        this.escolaridade = escolaridade;
    }

    public BigDecimal getRenda() {
        return renda;
    }

    public void setRenda(BigDecimal renda) {
        this.renda = renda;
    }

    public BigDecimal getBeneficios() {
        return beneficios;
    }

    public void setBeneficios(BigDecimal beneficios) {
        this.beneficios = beneficios;
    }
}