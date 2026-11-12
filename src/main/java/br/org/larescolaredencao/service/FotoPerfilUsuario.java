package br.org.larescolaredencao.service;

/**
 * Regras da foto de perfil dos usuários atendidos.
 *
 * As fotos ficam em {@code uploads/usuarios/} e não são servidas como recurso estático:
 * a consulta é feita por {@code GET /usuarios/{id}/foto}, que exige autenticação e acesso
 * à unidade do usuário. As respostas da API expõem somente essa rota protegida, nunca o
 * caminho físico do arquivo.
 */
public final class FotoPerfilUsuario {

    public static final String SUBPASTA = "usuarios/";
    public static final String PREFIXO_CAMINHO = "/uploads/" + SUBPASTA;

    private FotoPerfilUsuario() {
    }

    /**
     * Rota protegida da foto, ou {@code null} quando não há foto cadastrada. O parâmetro
     * {@code v} muda a cada troca de foto, evitando que o front reutilize uma imagem antiga.
     */
    public static String urlProtegida(Integer idUsuario, String caminhoArquivo) {
        if (idUsuario == null || caminhoArquivo == null || caminhoArquivo.isBlank()) {
            return null;
        }
        return "/usuarios/" + idUsuario + "/foto?v=" + Integer.toHexString(caminhoArquivo.hashCode());
    }

    /** A foto precisa estar diretamente em {@code /uploads/usuarios/}, sem subpastas nem navegação. */
    public static boolean caminhoPermitido(String caminhoArquivo) {
        if (caminhoArquivo == null || !caminhoArquivo.startsWith(PREFIXO_CAMINHO)) {
            return false;
        }
        String nome = caminhoArquivo.substring(PREFIXO_CAMINHO.length());
        return !nome.isBlank() && !nome.contains("/") && !nome.contains("\\") && !nome.contains("..");
    }
}
