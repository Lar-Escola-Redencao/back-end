package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.Parceiro;

public class ParceiroResponseDTO {

    private Long id;
    private String nome;
    private String logo;

    public ParceiroResponseDTO(Parceiro parceiro) {
        this.id = parceiro.getId();
        this.nome = parceiro.getNome();
        this.logo = parceiro.getLogo();
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getLogo() {
        return logo;
    }
}
