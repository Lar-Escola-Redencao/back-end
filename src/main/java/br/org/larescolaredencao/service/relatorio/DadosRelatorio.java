package br.org.larescolaredencao.service.relatorio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Dados já calculados, compartilhados pelos geradores de PDF e XLSX para manter os formatos equivalentes. */
public final class DadosRelatorio {
    private DadosRelatorio() {}

    public static final String INSTITUICAO = "Lar Escola Redenção";
    public static final String OBSERVACAO_PERCENTUAL = "Percentual calculado somente sobre os dias com frequência registrada. "
            + "Dias sem registro não são contabilizados como faltas.";
    public static final String SEM_FREQUENCIA = "Sem frequência registrada";
    public static final String SEM_OCORRENCIAS = "Nenhuma ocorrência registrada no período.";
    public static final String SEM_ALUNOS = "Nenhum usuário matriculado na turma no período.";
    public static final String OBSERVACAO_FORA_DA_MATRICULA = "Registro salvo em data fora da vigência da matrícula. "
            + "Exibido como consta no sistema, sem alteração.";

    public record Turma(String unidade, String descricao, String abreviacao) {}

    public record Aluno(String nome, boolean excluido) {
        public String nomeExibicao() {
            return excluido ? nome + " (excluído)" : nome;
        }
    }

    public record AlunoPresencas(Aluno aluno, Map<LocalDate, Indicador> indicadores) {}

    public record Ocorrencia(LocalDateTime dataHora, String aluno, String tipo, String descricao, String autor) {}

    public record Presencas(Turma turma, LocalDate inicio, LocalDate fim, LocalDateTime geradoEm,
                            List<LocalDate> datas, List<AlunoPresencas> alunos,
                            List<Ocorrencia> ocorrencias, boolean possuiRegistroForaDaMatricula) {
        public boolean incluiOcorrencias() {
            return ocorrencias != null;
        }
    }

    public record AlunoResumo(Aluno aluno, int presencas, int faltas, boolean possuiRegistroForaDaMatricula) {
        public int diasRegistrados() {
            return presencas + faltas;
        }

        /** Fração entre 0 e 1; nulo quando não há frequência registrada. */
        public Double percentual() {
            return diasRegistrados() == 0 ? null : (double) presencas / diasRegistrados();
        }

        /** Percentual com uma casa decimal, arredondado da mesma forma em todos os formatos. */
        public BigDecimal percentualArredondado() {
            return diasRegistrados() == 0 ? null : BigDecimal.valueOf(presencas * 100L)
                    .divide(BigDecimal.valueOf(diasRegistrados()), 1, RoundingMode.HALF_UP);
        }

        public String observacao() {
            StringBuilder texto = new StringBuilder();
            if (diasRegistrados() == 0) texto.append(SEM_FREQUENCIA);
            if (aluno.excluido()) texto.append(texto.isEmpty() ? "" : "; ").append("Aluno excluído do sistema");
            if (possuiRegistroForaDaMatricula) {
                texto.append(texto.isEmpty() ? "" : "; ").append("Possui registro fora da vigência da matrícula");
            }
            return texto.toString();
        }
    }

    public record TurmaResumo(Turma turma, List<AlunoResumo> alunos) {}

    public record Resumo(LocalDate inicio, LocalDate fim, LocalDateTime geradoEm, List<TurmaResumo> turmas) {}
}
