package br.org.larescolaredencao.config;

import java.util.List;

/**
 * Diretórios de {@code uploads/} cujo conteúdo é publicado no site institucional.
 *
 * Somente estes diretórios são servidos como recurso estático e liberados sem autenticação.
 * Qualquer outro diretório (como {@code usuarios/}, com fotos e documentos de saúde) não é
 * exposto: os arquivos internos são consultados apenas por endpoints que validam o acesso.
 */
public final class RecursosPublicos {

    public static final List<String> DIRETORIOS_UPLOAD = List.of(
            "parceiros",
            "diretoria",
            "redes-sociais",
            "eventos",
            "unidades",
            "transparencia",
            "paginas");

    private RecursosPublicos() {
    }

    public static String[] padroesUpload() {
        return DIRETORIOS_UPLOAD.stream().map(diretorio -> "/uploads/" + diretorio + "/**").toArray(String[]::new);
    }
}
