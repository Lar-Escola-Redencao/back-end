package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.EventoRedeSocial;

public class EventoRedeSocialResponseDTO {

    private Long idRedeSocial;
    private String nome;
    private String icone;
    private String urlLink;

    public EventoRedeSocialResponseDTO(EventoRedeSocial vinculo) {
        this.idRedeSocial = vinculo.getRedeSocial().getId();
        this.nome = vinculo.getRedeSocial().getNome();
        this.icone = vinculo.getRedeSocial().getIcone();
        this.urlLink = vinculo.getUrlLink();
    }

    public Long getIdRedeSocial() {
        return idRedeSocial;
    }

    public String getNome() {
        return nome;
    }

    public String getIcone() {
        return icone;
    }

    public String getUrlLink() {
        return urlLink;
    }
}
