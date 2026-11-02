package br.org.larescolaredencao.service;

import java.util.Set;

public enum TipoArquivo {

    DOCUMENTO(
            Set.of("pdf", "docx", "xlsx"),
            Set.of(
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ),
            0, 0f
    ),
    FOTO(
            Set.of("jpg", "jpeg", "png", "webp"),
            Set.of("image/jpeg", "image/png", "image/webp"),
            1920, 0.80f
    ),
    // Limite maior e qualidade mais alta para manter legível o texto de laudos e receitas fotografados.
    SAUDE(
            Set.of("pdf", "jpg", "jpeg", "png"),
            Set.of("application/pdf", "image/jpeg", "image/png"),
            2560, 0.90f
    );

    private final Set<String> extensoesPermitidas;
    private final Set<String> tiposMimePermitidos;
    private final int dimensaoMaxima;
    private final float qualidadeJpeg;

    TipoArquivo(Set<String> extensoesPermitidas, Set<String> tiposMimePermitidos,
                int dimensaoMaxima, float qualidadeJpeg) {
        this.extensoesPermitidas = extensoesPermitidas;
        this.tiposMimePermitidos = tiposMimePermitidos;
        this.dimensaoMaxima = dimensaoMaxima;
        this.qualidadeJpeg = qualidadeJpeg;
    }

    public Set<String> getExtensoesPermitidas() {
        return extensoesPermitidas;
    }
    public Set<String> getTiposMimePermitidos() {
        return tiposMimePermitidos;
    }
    /** Lado maior máximo, em pixels, das imagens gravadas. */
    public int getDimensaoMaxima() {
        return dimensaoMaxima;
    }
    public float getQualidadeJpeg() {
        return qualidadeJpeg;
    }
    public boolean comprimeImagens() {
        return dimensaoMaxima > 0;
    }
}
