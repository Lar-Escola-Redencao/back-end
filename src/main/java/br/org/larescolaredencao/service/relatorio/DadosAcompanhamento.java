package br.org.larescolaredencao.service.relatorio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Dados já consolidados do relatório individual de acompanhamento. */
public record DadosAcompanhamento(String usuario, LocalDate inicio, LocalDate fim, String filtroMatricula,
                                  LocalDateTime geradoEm, String responsavel,
                                  List<MatriculaRelatorio> matriculas,
                                  Map<LocalDate, List<RegistroDia>> registros,
                                  List<Ocorrencia> ocorrencias, List<Entrevista> entrevistas) {

    public static final String SEM_OCORRENCIAS = "Nenhuma ocorrência registrada no período.";
    public static final String SEM_ENTREVISTAS = "Nenhuma entrevista registrada no período.";

    public record MatriculaRelatorio(int id, String unidade, String turma, LocalDate ingresso, LocalDate desligamento) {
        public boolean vigente(LocalDate data) {
            return ingresso != null && !ingresso.isAfter(data) && (desligamento == null || !desligamento.isBefore(data));
        }
    }

    public record RegistroDia(int idMatricula, boolean presente) {}

    public record Ocorrencia(LocalDateTime dataHora, String tipo, String descricao, String autor, String unidadeTurma) {}

    public record Entrevista(LocalDateTime dataHora, String descricao, String autor) {}

    public enum Situacao { FORA_DO_PERIODO, SEM_MATRICULA, SEM_REGISTRO, COM_REGISTRO }

    public Situacao situacao(LocalDate data) {
        if (data.isBefore(inicio) || data.isAfter(fim)) return Situacao.FORA_DO_PERIODO;
        if (!registros.getOrDefault(data, List.of()).isEmpty()) return Situacao.COM_REGISTRO;
        return matriculas.stream().anyMatch(m -> m.vigente(data)) ? Situacao.SEM_REGISTRO : Situacao.SEM_MATRICULA;
    }

    /** As entrevistas só existem no relatório de perfis autorizados; para os demais a seção é omitida. */
    public boolean incluiEntrevistas() {
        return entrevistas != null;
    }
}
