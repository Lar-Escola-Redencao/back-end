package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.MidiaEvento;
import br.org.larescolaredencao.model.enums.TipoMidia;

public class MidiaEventoResponseDTO {

    private Integer id;
    private TipoMidia tipoMidia;
    private String urlMidia;

    public MidiaEventoResponseDTO(MidiaEvento midia) {
        this.id = midia.getId();
        this.tipoMidia = midia.getTipoMidia();
        this.urlMidia = midia.getUrlMidia();
    }

    public Integer getId() {
        return id;
    }

    public TipoMidia getTipoMidia() {
        return tipoMidia;
    }

    public String getUrlMidia() {
        return urlMidia;
    }
}
