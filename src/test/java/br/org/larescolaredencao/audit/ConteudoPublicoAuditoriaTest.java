package br.org.larescolaredencao.audit;

import br.org.larescolaredencao.model.enums.TipoRegistroHistorico;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConteudoPublicoAuditoriaTest {

    @Test
    void paginasSecoesEDocumentosPermanecemNaAreaDaPaginaDeOrigem() {
        var categorias = Map.of(1, TipoRegistroHistorico.TRANSPARENCIA,
                2, TipoRegistroHistorico.CONHECA_OSC, 3, TipoRegistroHistorico.GRAFICA,
                4, TipoRegistroHistorico.PIX);
        for (var entidade : List.of(EntidadeAuditada.PAGINA, EntidadeAuditada.SECAO, EntidadeAuditada.DOCUMENTO)) {
            categorias.forEach((pagina, categoria) -> assertThat(ConteudoPublicoAuditoria.categoria(
                    entidade, Map.of("idPagina", pagina.longValue()))).isEqualTo(categoria));
        }
    }

    @Test
    void publicacoesNasRedesSociaisFicamNoHistoricoDoEvento() {
        assertThat(ConteudoPublicoAuditoria.categoria(EntidadeAuditada.LINK_EVENTO, Map.of()))
                .isEqualTo(TipoRegistroHistorico.EVENTOS);
        assertThat(ConteudoPublicoAuditoria.categoria(EntidadeAuditada.REDE_SOCIAL, Map.of()))
                .isEqualTo(TipoRegistroHistorico.REDES_SOCIAIS);
    }

    @Test
    void naoAtribuiDocumentoSemPaginaATransparenciaPorEngano() {
        assertThatThrownBy(() -> ConteudoPublicoAuditoria.categoria(EntidadeAuditada.DOCUMENTO, Map.of()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("identificar a página");
    }
}
