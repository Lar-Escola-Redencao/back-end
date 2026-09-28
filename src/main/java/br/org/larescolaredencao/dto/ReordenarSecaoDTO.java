package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.NotNull;

public class ReordenarSecaoDTO {

    @NotNull
    private Long id;

    @NotNull
    private Integer ordem;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getOrdem() {
        return ordem;
    }

    public void setOrdem(Integer ordem) {
        this.ordem = ordem;
    }
}
