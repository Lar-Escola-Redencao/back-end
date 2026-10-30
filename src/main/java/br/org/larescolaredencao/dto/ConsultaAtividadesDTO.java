package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.enums.PeriodoHistorico;
import br.org.larescolaredencao.model.enums.AreaSistemaHistorico;
import br.org.larescolaredencao.model.enums.TipoAlteracaoHistorico;
import br.org.larescolaredencao.model.enums.TipoRegistroHistorico;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record ConsultaAtividadesDTO(
        TipoRegistroHistorico tipoRegistro,
        @Size(max = 200) String search,
        @Positive Integer idRegistro,
        @Positive Integer idResponsavel,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
        TipoAlteracaoHistorico tipoAlteracao,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAula,
        @Positive Integer idUsuario,
        @Positive Integer idTurma,
        @Positive Integer idContato,
        PeriodoHistorico periodo,
        AreaSistemaHistorico areaSistema) {

    public PeriodoHistorico periodoEfetivo() {
        return periodo == null ? PeriodoHistorico.RECENTES : periodo;
    }

    public String contextoLeitura() {
        String contexto = String.join(":", String.valueOf(tipoRegistro), String.valueOf(idRegistro),
                String.valueOf(idUsuario), String.valueOf(idTurma), String.valueOf(idContato), String.valueOf(dataAula),
                periodoEfetivo().name());
        return areaSistema == null ? contexto : contexto + ":" + areaSistema.name();
    }
}
