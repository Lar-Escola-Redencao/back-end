package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.enums.TipoAlteracaoHistorico;
import br.org.larescolaredencao.model.enums.TipoRegistroHistorico;

import java.time.OffsetDateTime;

public record HistoricoEventoDTO(
        long id,
        int revisao,
        OffsetDateTime dataHora,
        Integer idResponsavel,
        String nomeResponsavel,
        TipoRegistroHistorico tipoRegistro,
        String informacaoAlterada,
        Integer idRegistro,
        String identificacaoRegistro,
        boolean registroExcluido,
        TipoAlteracaoHistorico tipoAlteracao) {
}
