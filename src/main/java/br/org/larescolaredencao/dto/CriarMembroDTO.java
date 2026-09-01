package br.org.larescolaredencao.dto;

//import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CriarMembroDTO {
	@NotBlank
    @Size(max = 150)
    private String nomeCompleto;
	
	@NotBlank
    @Email
    @Size(max = 100)
    private String email;
	
	@NotBlank
    private String senha;
	
	@NotBlank
    @Size(max = 14)
    private String cpf;
    private String endereco;
    private String telefone;
    
    @NotNull
    private Integer idPapel;
    
    // @NotEmpty(message = "Selecione ao menos uma unidade de negócio.")
    // private List<Integer> idsUnidades;

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

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
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
    
    // public List<Integer> getIdsUnidades() {
    //     return idsUnidades;
    // }
    //
    // public void setIdsUnidades(List<Integer> idsUnidades) {
    //     this.idsUnidades = idsUnidades;
    // }
}