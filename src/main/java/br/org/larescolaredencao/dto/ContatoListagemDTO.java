package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.Contato;

import java.util.List;

public class ContatoListagemDTO {

    private Integer id;
    private String nomeCompleto;
    private String telefone;
    private String email;
    private String endereco;
    private long quantidadeVinculos;
    private List<VinculoContatoResponseDTO> vinculos;

    public ContatoListagemDTO(Contato contato, long quantidadeVinculos) {
        this.id = contato.getId();
        this.nomeCompleto = contato.getNomeCompleto();
        this.telefone = contato.getTelefone();
        this.email = contato.getEmail();
        this.endereco = contato.getEndereco();
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