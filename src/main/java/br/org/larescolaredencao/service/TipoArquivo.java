package br.org.larescolaredencao.service;

import java.util.Set;

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
    ),
    SAUDE(
            Set.of("pdf", "jpg", "jpeg", "png"),
            Set.of("application/pdf", "image/jpeg", "image/png")
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