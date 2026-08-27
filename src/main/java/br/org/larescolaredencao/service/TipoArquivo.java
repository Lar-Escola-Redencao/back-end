package br.org.larescolaredencao.service;

import java.util.Set;

/**
 * Categorias de arquivo suportadas pelo armazenamento genérico, cada uma com sua
 * própria lista branca de extensões e tipos MIME. Cada chamador de
 * {@link ArquivoService} escolhe a categoria adequada ao seu caso de uso.
 */
public enum TipoArquivo {

    DOCUMENTO(
            Set.of("pdf", "docx", "xlsx"),
            Set.of(
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
    ),
    FOTO(
            Set.of("jpg", "jpeg", "png", "webp"),
            Set.of("image/jpeg", "image/png", "image/webp")
    );

    private final Set<String> extensoesPermitidas;
    private final Set<String> tiposMimePermitidos;

    TipoArquivo(Set<String> extensoesPermitidas, Set<String> tiposMimePermitidos) {
        this.extensoesPermitidas = extensoesPermitidas;
        this.tiposMimePermitidos = tiposMimePermitidos;
    }

    public Set<String> getExtensoesPermitidas() {
        return extensoesPermitidas;
    }
    public Set<String> getTiposMimePermitidos() {
        return tiposMimePermitidos;
    }
}
