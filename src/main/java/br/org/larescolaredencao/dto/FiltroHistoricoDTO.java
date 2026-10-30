package br.org.larescolaredencao.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import br.org.larescolaredencao.model.enums.TipoRegistroHistorico;
import br.org.larescolaredencao.model.enums.TipoAlteracaoHistorico;

public record FiltroHistoricoDTO(
        @NotNull TipoRegistroHistorico tipoRegistro,
        @Size(max = 200) String search,
        @Positive Integer idRegistro,
        @Positive Integer idResponsavel,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
        TipoAlteracaoHistorico tipoAlteracao,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAula) {
}
