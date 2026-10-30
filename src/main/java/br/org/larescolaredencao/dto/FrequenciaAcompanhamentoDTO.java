package br.org.larescolaredencao.dto;

import java.time.LocalDate;
import java.util.List;

public record FrequenciaAcompanhamentoDTO(LocalDate dataInicio, LocalDate dataFim, List<Mes> meses,
        long presencas, long faltas, long totalRegistros) {
    public record Mes(int ano, int mes, long presencas, long faltas) {}
}
