package br.org.larescolaredencao.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Size;

public class AtualizarSecaoDTO {

    @Size(min = 3, max = 150)
    private String titulo;

    private String conteudo;

    /** Opcional — só substitui a imagem atual quando enviada. */
    private MultipartFile imagem;

    private Boolean ativo;

    public String getTitulo() {
        return titulo;
    }
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }
    public String getConteudo() {
        return conteudo;
    }
    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }
    public MultipartFile getImagem() {
        return imagem;
    }
    public void setImagem(MultipartFile imagem) {
        this.imagem = imagem;
    }
    public Boolean getAtivo() {
        return ativo;
    }
    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
}
