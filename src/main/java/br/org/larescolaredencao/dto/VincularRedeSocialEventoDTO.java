package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class VincularRedeSocialEventoDTO {

    @NotNull
    private Long idRedeSocial;

    // Opcional: se vier vazio, o service usa a url cadastrada na rede social.
    @Size(max = 255, message = "O link não pode ter mais de 255 caracteres")
    private String urlLink;

    public Long getIdRedeSocial() {
        return idRedeSocial;
    }

    public void setIdRedeSocial(Long idRedeSocial) {
        this.idRedeSocial = idRedeSocial;
    }

    public String getUrlLink() {
        return urlLink;
    }

    public void setUrlLink(String urlLink) {
        this.urlLink = urlLink;
    }
}
