package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class AtualizarMembroDTO {
	
    @Size(max = 150)
    private String nomeCompleto;
    
    @Email
    @Size(max = 100)
    private String email;
    
    @Size(max = 14)
    private String cpf;
    private String endereco;
    private String telefone;
    private Integer idPapel;
    
    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public Integer getIdPapel() {
        return idPapel;
    }

    public void setIdPapel(Integer idPapel) {
        this.idPapel = idPapel;
    }
}