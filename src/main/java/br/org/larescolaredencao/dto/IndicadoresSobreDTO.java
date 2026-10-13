package br.org.larescolaredencao.dto;

import java.time.LocalDate;

public class IndicadoresSobreDTO {
    private final long totalMeninos;
    private final LocalDate dataFundacao;

    public IndicadoresSobreDTO(long totalMeninos, LocalDate dataFundacao) {
        this.totalMeninos = totalMeninos;
        this.dataFundacao = dataFundacao;
    }

    public long getTotalMeninos() {
        return totalMeninos;
    }

    public LocalDate getDataFundacao() {
        return dataFundacao;
    }
}
