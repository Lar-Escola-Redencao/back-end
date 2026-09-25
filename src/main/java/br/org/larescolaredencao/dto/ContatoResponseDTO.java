package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.ContatoAssistido;
import br.org.larescolaredencao.model.enums.Parentesco;

public class ContatoResponseDTO {

    private Integer id;
    private String nomeCompleto;
    private String telefone;
    private String email;
    private String endereco;
    private Parentesco parentesco;
    private Boolean principal;

    public ContatoResponseDTO(ContatoAssistido ca) {
        this.id = ca.getContato().getId();
        this.nomeCompleto = ca.getContato().getNomeCompleto();
        this.telefone = ca.getContato().getTelefone();
        this.email = ca.getContato().getEmail();
        this.endereco = ca.getContato().getEndereco();
        this.parentesco = ca.getParentesco();
        this.principal = ca.getPrincipal();
    }

    public Integer getId() {
        return id;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getEmail() {
        return email;
    }

    public String getEndereco() {
        return endereco;
    }

    public Parentesco getParentesco() {
        return parentesco;
    }

    public Boolean getPrincipal() {
        return principal;
    }
}