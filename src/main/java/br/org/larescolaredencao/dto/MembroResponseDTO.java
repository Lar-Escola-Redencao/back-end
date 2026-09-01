package br.org.larescolaredencao.dto;

//import java.util.List;

import br.org.larescolaredencao.model.Membro;
//import br.org.larescolaredencao.model.Unidade;

public class MembroResponseDTO {
    private Integer id;
    private String nomeCompleto;
    private String email;
    private String cpf;
    private String endereco;
    private String telefone;
    private Integer idPapel;
    private String nomePapel;
    // private List<Unidade> unidades;

    public MembroResponseDTO(Membro membro) {
        this.id = membro.getId();
        this.nomeCompleto = membro.getNomeCompleto();
        this.email = membro.getEmail();
        this.cpf = membro.getCpf();
        this.endereco = membro.getEndereco();
        this.telefone = membro.getTelefone();
        this.idPapel = membro.getPapel().getId();
        this.nomePapel = membro.getPapel().getNomePapel();
        // this.unidades = membro.getUnidades();
    }

    public Integer getId() {
        return id;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public String getEmail() {
        return email;
    }

    public String getCpf() {
        return cpf;
    }

    public String getEndereco() {
        return endereco;
    }

    public String getTelefone() {
        return telefone;
    }

    public Integer getIdPapel() {
        return idPapel;
    }

    public String getNomePapel() {
        return nomePapel;
    }
    
    // public List<Unidade> getUnidades() {
    //     return unidades;
    // }
}