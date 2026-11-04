package br.org.larescolaredencao.audit;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ComparadorAuditoriaTest {

    @Test
    void estadosEquivalentesIgnoramVazioECasasDecimais() {
        Map<String, Object> anterior = new LinkedHashMap<>();
        anterior.put("id", 1);
        anterior.put("cep", null);
        anterior.put("renda", new BigDecimal("100.00"));
        Map<String, Object> novo = Map.of("id", 1L, "cep", "", "renda", new BigDecimal("100.0"));

        assertThat(ComparadorAuditoria.equivalentes(anterior, novo)).isTrue();
        assertThat(ComparadorAuditoria.equivalentes(novo, anterior)).isTrue();
    }

    @Test
    void cadastroExclusaoERemocaoDeValorPreenchidoContinuamDiferentes() {
        var cadastro = Map.of("id", 1, "localTrabalho", "Escola");
        var semLocal = Map.of("id", 1, "localTrabalho", "");

        assertThat(ComparadorAuditoria.equivalentes(null, cadastro)).isFalse();
        assertThat(ComparadorAuditoria.equivalentes(cadastro, null)).isFalse();
        assertThat(ComparadorAuditoria.equivalentes(cadastro, semLocal)).isFalse();
        assertThat(ComparadorAuditoria.equivalentes(new BigDecimal("100"), new BigDecimal("101"))).isFalse();
    }

    @Test
    void equivalenciaNaoRemoveDiferencasReaisDeTextoOuVinculos() {
        assertThat(ComparadorAuditoria.equivalentes("Ana", " Ana ")).isFalse();
        assertThat(ComparadorAuditoria.equivalentes(
                Map.of("unidades", java.util.List.of(1)),
                Map.of("unidades", java.util.List.of(2)))).isFalse();
    }
}
