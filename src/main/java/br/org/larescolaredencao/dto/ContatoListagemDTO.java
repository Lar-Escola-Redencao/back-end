package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.Contato;

import java.util.List;

public class ContatoListagemDTO {

    private Integer id;
    private String nomeCompleto;
    private String telefone;
    private String email;
    private String endereco;
    private String cpf;
    private String localTrabalho;
    private long quantidadeVinculos;
    private List<VinculoContatoResponseDTO> vinculos;

    public ContatoListagemDTO(Contato contato, long quantidadeVinculos) {
        this.id = contato.getId();
        this.nomeCompleto = contato.getNomeCompleto();
        this.telefone = contato.getTelefone();
        this.email = contato.getEmail();
        this.endereco = contato.getEndereco();
        this.cpf = contato.getCpf();
        this.localTrabalho = contato.getLocalTrabalho();
        this.quantidadeVinculos = quantidadeVinculos;
    }

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

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getLocalTrabalho() {
        return localTrabalho;
    }

    public void setLocalTrabalho(String localTrabalho) {
        this.localTrabalho = localTrabalho;
    }

    public long getQuantidadeVinculos() {
        return quantidadeVinculos;
    }

    public void setQuantidadeVinculos(long quantidadeVinculos) {
        this.quantidadeVinculos = quantidadeVinculos;
    }

    public List<VinculoContatoResponseDTO> getVinculos() {
        return vinculos;
    }

    public void setVinculos(List<VinculoContatoResponseDTO> vinculos) {
        this.vinculos = vinculos;
    }
}