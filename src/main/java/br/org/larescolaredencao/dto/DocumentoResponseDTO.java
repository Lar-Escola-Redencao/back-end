package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.Documento;

public class DocumentoResponseDTO {

    private Long id;
    private String titulo;
    private String arquivo;
    private Long secaoId;
    private String secaoTitulo;

    public DocumentoResponseDTO(Documento documento) {
        this.id = documento.getId();
        this.titulo = documento.getTitulo();
        this.arquivo = documento.getArquivo();
        this.secaoId = documento.getSecao().getId();
        this.secaoTitulo = documento.getSecao().getTitulo();
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getArquivo() {
        return arquivo;
    }

    public Long getSecaoId() {
        return secaoId;
    }

    public String getSecaoTitulo() {
        return secaoTitulo;
    }
}
