package br.org.larescolaredencao.dto;

import java.util.List;

public record AtividadeHistoricoDTO(
        HistoricoEventoDTO evento,
        String resumo,
        List<HistoricoEventoDTO> registros) {
}
