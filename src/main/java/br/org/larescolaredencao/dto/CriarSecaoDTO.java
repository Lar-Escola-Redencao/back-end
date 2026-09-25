package br.org.larescolaredencao.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Size;

public class CriarSecaoDTO {

    @Size(min = 3, max = 150)
    private String titulo;

    private String conteudo;

    private String grupo;

    private Integer ordem;

    private MultipartFile imagem;

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

    public String getGrupo() {
        return grupo;
    }

    public void setGrupo(String grupo) {
        this.grupo = grupo;
    }

    public Integer getOrdem() {
        return ordem;
    }

    public void setOrdem(Integer ordem) {
        this.ordem = ordem;
    }

    public MultipartFile getImagem() {
        return imagem;
    }

    public void setImagem(MultipartFile imagem) {
        this.imagem = imagem;
    }
}
