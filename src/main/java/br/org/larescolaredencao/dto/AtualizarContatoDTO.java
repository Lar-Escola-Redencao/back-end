package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.validacao.CpfValido;

import jakarta.validation.constraints.NotBlank;

public class AtualizarContatoDTO {

    @NotBlank
    private String nomeCompleto;

    @NotBlank
    private String telefone;

    private String email;
    
    private String endereco;

    @CpfValido
    private String cpf;

    private String localTrabalho;

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf == null || cpf.isBlank() ? null : cpf.trim();
    }

    public String getLocalTrabalho() {
        return localTrabalho;
    }

    public void setLocalTrabalho(String localTrabalho) {
        this.localTrabalho = localTrabalho;
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
}
