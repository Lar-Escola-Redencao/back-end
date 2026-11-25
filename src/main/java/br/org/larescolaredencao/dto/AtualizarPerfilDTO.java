package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.validacao.CpfValido;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;

public class AtualizarPerfilDTO {

    @NotBlank
    @Size(max = 150)
    private String nomeCompleto;

    private String endereco;

    private String telefone;

    @Email
    @Size(min = 5, max = 100)
    private String email;

    @CpfValido
    private String cpf;

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
        this.cpf = cpf == null || cpf.isBlank() ? null : cpf.trim();
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
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
    
}
