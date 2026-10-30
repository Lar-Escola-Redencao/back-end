package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class CodigoPixTest {

    @Test
    void deveCalcularVetorDeReferenciaCrc16CcittFalse() {
        assertThat(CodigoPix.crc16("123456789")).isEqualTo("29B1");
    }

    @Test
    void deveCodificarChaveValorRecebedorEIdentificadorNoBrCode() {
        String payload = CodigoPix.gerar("DOACOES@example.org", new BigDecimal("25.50"),
                "Lar Escola Redenção", "Araraquara", "LER123");
        assertThat(payload).startsWith("000201").contains("br.gov.bcb.pix", "doacoes@example.org",
                "540525.50", "5802BR", "5919LAR ESCOLA REDENCAO", "6010ARARAQUARA", "62100506LER123");
        assertThat(payload).endsWith(CodigoPix.crc16(payload.substring(0, payload.length() - 4)));
    }

    @Test
    void deveNormalizarDocumentosTelefoneEChaveAleatoria() {
        assertThat(CodigoPix.normalizarChave("529.982.247-25")).isEqualTo("52998224725");
        assertThat(CodigoPix.normalizarChave("11.222.333/0001-81")).isEqualTo("11222333000181");
        assertThat(CodigoPix.normalizarChave("12.ABC.345/01DE-35")).isEqualTo("12ABC34501DE35");
        assertThat(CodigoPix.normalizarChave("+55 (16) 99999-9999")).isEqualTo("+5516999999999");
        assertThat(CodigoPix.normalizarChave("123E4567-E12B-12D1-A456-426655440000"))
                .isEqualTo("123e4567-e12b-12d1-a456-426655440000");
    }

    @Test
    void deveRecusarChavesMalformadasEDocumentosComDigitoInvalido() {
        for (String chave : new String[] { "", "chave-invalida", "52998224724", "11111111111", "doar@", "+000" }) {
            assertThatThrownBy(() -> CodigoPix.normalizarChave(chave)).isInstanceOf(ResponseStatusException.class);
        }
    }

    @Test
    void deveRecusarValoresInvalidosSemArredondar() {
        for (String valor : new String[] { "0", "-1", "1000000", "10.001" }) {
            assertThatThrownBy(() -> CodigoPix.gerar("doacoes@example.org", new BigDecimal(valor),
                    "Lar Escola Redencao", "Araraquara", "LER123"))
                    .isInstanceOf(ResponseStatusException.class);
        }
        assertThat(CodigoPix.gerar("doacoes@example.org", new BigDecimal("0.01"),
                "Lar Escola Redencao", "Araraquara", "LER123")).contains("54040.01");
    }
}
