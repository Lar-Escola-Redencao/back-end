package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.enums.EscolaridadeFamiliar;
import br.org.larescolaredencao.model.enums.Parentesco;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ComposicaoFamiliarDTO {

    @NotBlank
    private String nomeCompleto;

    @NotNull
    private Parentesco parentescoVinculo;

    private Integer idade;

    @NotNull
    private EscolaridadeFamiliar escolaridade;

    private BigDecimal renda;

    private BigDecimal beneficios;

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