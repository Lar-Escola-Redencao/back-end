package br.org.larescolaredencao.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CriarSecaoDTO {

    @NotBlank
    @Size(min = 3, max = 150)
    private String titulo;

    private String conteudo;

    /** Opcional — nem toda seção precisa de uma imagem. */
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
    public MultipartFile getImagem() {
        return imagem;
    }
    public void setImagem(MultipartFile imagem) {
        this.imagem = imagem;
    }
}
