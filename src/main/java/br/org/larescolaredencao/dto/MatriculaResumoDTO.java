package br.org.larescolaredencao.dto;

public class MatriculaResumoDTO {
    private final long totalMeninos;

    public MatriculaResumoDTO(long totalMeninos) {
        this.totalMeninos = totalMeninos;
    }

    public long getTotalMeninos() {
        return totalMeninos;
    }
}
