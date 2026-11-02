package br.org.larescolaredencao.audit;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;

public final class ComparadorAuditoria {

    private ComparadorAuditoria() {
    }

    public static boolean equivalentes(Object anterior, Object novo) {
        if (vazio(anterior) && vazio(novo)) {
            return true;
        }
        if (anterior instanceof Number numeroAnterior && novo instanceof Number numeroNovo) {
            return new BigDecimal(numeroAnterior.toString())
                    .compareTo(new BigDecimal(numeroNovo.toString())) == 0;
        }
        if (anterior instanceof Map<?, ?> mapaAnterior && novo instanceof Map<?, ?> mapaNovo) {
            var campos = new LinkedHashSet<Object>(mapaAnterior.keySet());
            campos.addAll(mapaNovo.keySet());
            return campos.stream().allMatch(campo -> equivalentes(mapaAnterior.get(campo), mapaNovo.get(campo)));
        }
        return Objects.equals(anterior, novo);
    }

    private static boolean vazio(Object valor) {
        return valor == null || valor instanceof String texto && texto.isBlank();
    }
}
