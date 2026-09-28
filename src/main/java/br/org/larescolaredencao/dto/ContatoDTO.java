package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.enums.Parentesco;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ContatoDTO {

    private Integer id;

    @NotBlank
    private String nomeCompleto;

    @NotBlank
    private String telefone;

    private String email;
    
    private String endereco;

    @NotNull
    private Parentesco parentesco;

    @NotNull
    private Boolean principal;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
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