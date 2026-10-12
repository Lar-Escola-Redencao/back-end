package br.org.larescolaredencao.service;

/**
 * Prepara o termo da barra de busca para as consultas com LIKE dos repositórios.
 * As queries usam ESCAPE '!', então os curingas digitados pelo usuário (% e _) são
 * tratados como texto literal.
 */
public final class TermoBusca {

    private TermoBusca() {
    }

    /** Termo em minúsculas envelopado como %termo%, ou null quando a busca não foi informada. */
    public static String like(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return "%" + escapar(search.trim().toLowerCase()) + "%";
    }

    /**
     * Apenas os dígitos do termo (%12345%), para casar CPF/telefone gravados sem máscara.
     * Só vale quando o termo é numérico (ex.: "123.456-78"); com letras retorna null,
     * senão "Rua 7" casaria qualquer CPF que contenha 7.
     */
    public static String digitosLike(String search) {
        if (search == null || !search.trim().matches("[\\d\\s.\\-()/]+")) {
            return null;
        }
        String digitos = search.replaceAll("\\D", "");
        return digitos.isEmpty() ? null : "%" + digitos + "%";
    }

    private static String escapar(String termo) {
        return termo.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
