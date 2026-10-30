package br.org.larescolaredencao.service.relatorio;

public enum Indicador {
    PRESENTE("P"),
    FALTA("F"),
    // Registros salvos em datas fora da vigência da matrícula são exibidos, nunca corrigidos.
    PRESENTE_FORA_DA_MATRICULA("P*"),
    FALTA_FORA_DA_MATRICULA("F*"),
    NAO_REGISTRADO("—"),
    NAO_MATRICULADO("N/A");

    private final String sigla;

    Indicador(String sigla) {
        this.sigla = sigla;
    }

    public String getSigla() {
        return sigla;
    }

    public boolean isPresenca() {
        return this == PRESENTE || this == PRESENTE_FORA_DA_MATRICULA;
    }

    public boolean isFalta() {
        return this == FALTA || this == FALTA_FORA_DA_MATRICULA;
    }

    public boolean isForaDaMatricula() {
        return this == PRESENTE_FORA_DA_MATRICULA || this == FALTA_FORA_DA_MATRICULA;
    }
}
