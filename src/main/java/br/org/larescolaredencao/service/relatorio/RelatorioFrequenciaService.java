package br.org.larescolaredencao.service.relatorio;

import br.org.larescolaredencao.dto.RelatorioPresencasDTO;
import br.org.larescolaredencao.dto.RelatorioResumoDTO;
import br.org.larescolaredencao.model.Frequencia;
import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Ocorrencia;
import br.org.larescolaredencao.model.Turma;
import br.org.larescolaredencao.model.Unidade;
import br.org.larescolaredencao.model.Usuario;
import br.org.larescolaredencao.model.enums.FormatoRelatorio;
import br.org.larescolaredencao.model.enums.Perfil;
import br.org.larescolaredencao.model.enums.Periodo;
import br.org.larescolaredencao.model.enums.StatusMatricula;
import br.org.larescolaredencao.model.enums.TipoOcorrencia;
import br.org.larescolaredencao.repository.FrequenciaRepository;
import br.org.larescolaredencao.repository.MatriculaRepository;
import br.org.larescolaredencao.repository.OcorrenciaRepository;
import br.org.larescolaredencao.repository.TurmaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.text.Collator;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class RelatorioFrequenciaService {

    // Limites operacionais: acima deles a exportação é recusada com mensagem, nunca truncada.
    public static final int LIMITE_DIAS_PERIODO = 366;
    public static final int LIMITE_TURMAS_RESUMO = 30;

    static final String TIPO_PDF = "application/pdf";
    static final String TIPO_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final Logger log = LoggerFactory.getLogger(RelatorioFrequenciaService.class);
    private static final ZoneId FUSO_BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final TurmaRepository turmaRepository;
    private final MatriculaRepository matriculaRepository;
    private final FrequenciaRepository frequenciaRepository;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final RelatorioPdfGerador pdfGerador;
    private final RelatorioXlsxGerador xlsxGerador;

    public RelatorioFrequenciaService(TurmaRepository turmaRepository,
                                      MatriculaRepository matriculaRepository,
                                      FrequenciaRepository frequenciaRepository,
                                      OcorrenciaRepository ocorrenciaRepository,
                                      RelatorioPdfGerador pdfGerador,
                                      RelatorioXlsxGerador xlsxGerador) {
        this.turmaRepository = turmaRepository;
        this.matriculaRepository = matriculaRepository;
        this.frequenciaRepository = frequenciaRepository;
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.pdfGerador = pdfGerador;
        this.xlsxGerador = xlsxGerador;
    }

    @Transactional(readOnly = true)
    public ArquivoRelatorio gerarPresencas(RelatorioPresencasDTO dto, Membro membroLogado) {
        validarPeriodo(dto.getDataInicio(), dto.getDataFim());
        Turma turma = buscarTurmaComAcesso(dto.getIdTurma(), membroLogado);
        DadosRelatorio.Presencas dados = montarPresencas(turma, dto.getDataInicio(), dto.getDataFim(),
                dto.isIncluirOcorrencias(), agora());

        String nome = "presencas-detalhadas_" + slug(turma.getUnidade().getNome()) + "_"
                + slug(periodo(turma.getPeriodo())) + "_" + dto.getDataInicio() + "_a_" + dto.getDataFim();
        return gerarArquivo(nome, dto.getFormato(),
                () -> pdfGerador.presencas(dados), () -> xlsxGerador.presencas(dados));
    }

    @Transactional(readOnly = true)
    public ArquivoRelatorio gerarResumo(RelatorioResumoDTO dto, Membro membroLogado) {
        validarPeriodo(dto.getDataInicio(), dto.getDataFim());
        Set<Integer> ids = new LinkedHashSet<>(dto.getIdsTurmas());
        if (ids.size() > LIMITE_TURMAS_RESUMO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Selecione no máximo " + LIMITE_TURMAS_RESUMO + " turmas por exportação.");
        }
        List<Turma> turmas = new ArrayList<>();
        for (Integer id : ids) {
            turmas.add(buscarTurmaComAcesso(id, membroLogado));
        }
        Collator collator = collator();
        turmas.sort(Comparator.comparing((Turma t) -> t.getUnidade().getNome(), collator)
                .thenComparing(Turma::getHoraInicio)
                .thenComparing(Turma::getId));

        List<DadosRelatorio.TurmaResumo> resumos = turmas.stream()
                .map(turma -> montarResumo(turma, dto.getDataInicio(), dto.getDataFim()))
                .toList();
        DadosRelatorio.Resumo dados = new DadosRelatorio.Resumo(dto.getDataInicio(), dto.getDataFim(), agora(), resumos);

        Set<Integer> unidades = new HashSet<>();
        turmas.forEach(t -> unidades.add(t.getUnidade().getId()));
        String escopo = turmas.size() == 1
                ? slug(turmas.get(0).getUnidade().getNome()) + "_" + slug(periodo(turmas.get(0).getPeriodo()))
                : unidades.size() == 1 ? slug(turmas.get(0).getUnidade().getNome()) + "_" + turmas.size() + "-turmas"
                : "varias-unidades_" + turmas.size() + "-turmas";
        String nome = "resumo-frequencia_" + escopo + "_" + dto.getDataInicio() + "_a_" + dto.getDataFim();
        return gerarArquivo(nome, dto.getFormato(),
                () -> pdfGerador.resumo(dados), () -> xlsxGerador.resumo(dados));
    }

    DadosRelatorio.Presencas montarPresencas(Turma turma, LocalDate inicio, LocalDate fim,
                                             boolean incluirOcorrencias, LocalDateTime geradoEm) {
        List<Ocorrencia> ocorrencias = ocorrenciaRepository.findPorTurmaNoPeriodo(
                turma.getId(), inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay());
        List<AlunoDaTurma> alunos = carregarAlunos(turma, inicio, fim, ocorrencias);
        List<LocalDate> datas = inicio.datesUntil(fim.plusDays(1)).toList();

        boolean foraDaMatricula = false;
        List<DadosRelatorio.AlunoPresencas> linhas = new ArrayList<>();
        for (AlunoDaTurma aluno : alunos) {
            Map<LocalDate, Indicador> indicadores = new LinkedHashMap<>();
            for (LocalDate data : datas) {
                Indicador indicador = aluno.indicador(data);
                foraDaMatricula |= indicador.isForaDaMatricula();
                indicadores.put(data, indicador);
            }
            linhas.add(new DadosRelatorio.AlunoPresencas(aluno.dados(), indicadores));
        }

        List<DadosRelatorio.Ocorrencia> ocorrenciasRelatorio = null;
        if (incluirOcorrencias) {
            Map<Integer, DadosRelatorio.Aluno> porUsuario = new LinkedHashMap<>();
            alunos.forEach(a -> porUsuario.put(a.idUsuario, a.dados()));
            Collator collator = collator();
            ocorrenciasRelatorio = ocorrencias.stream()
                    .sorted(Comparator.comparing(Ocorrencia::getDataOcorrencia)
                            .thenComparing(o -> o.getMatricula().getUsuario().getNomeCompleto(), collator)
                            .thenComparing(Ocorrencia::getId))
                    .map(o -> new DadosRelatorio.Ocorrencia(
                            o.getDataOcorrencia(),
                            porUsuario.get(o.getMatricula().getUsuario().getId()).nomeExibicao(),
                            rotulo(o.getTipoOcorrencia()),
                            o.getDescricao(),
                            // O membro da ocorrência não é atualizável: sempre o autor original.
                            o.getMembro().getNomeCompleto()))
                    .toList();
        }

        return new DadosRelatorio.Presencas(descricao(turma), inicio, fim, geradoEm, datas, linhas,
                ocorrenciasRelatorio, foraDaMatricula);
    }

    DadosRelatorio.TurmaResumo montarResumo(Turma turma, LocalDate inicio, LocalDate fim) {
        List<Ocorrencia> ocorrencias = ocorrenciaRepository.findPorTurmaNoPeriodo(
                turma.getId(), inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay());
        List<DadosRelatorio.AlunoResumo> linhas = new ArrayList<>();
        for (AlunoDaTurma aluno : carregarAlunos(turma, inicio, fim, ocorrencias)) {
            int presencas = 0;
            int faltas = 0;
            boolean foraDaMatricula = false;
            for (LocalDate data : inicio.datesUntil(fim.plusDays(1)).toList()) {
                Indicador indicador = aluno.indicador(data);
                if (indicador.isPresenca()) presencas++;
                if (indicador.isFalta()) faltas++;
                foraDaMatricula |= indicador.isForaDaMatricula();
            }
            linhas.add(new DadosRelatorio.AlunoResumo(aluno.dados(), presencas, faltas, foraDaMatricula));
        }
        return new DadosRelatorio.TurmaResumo(descricao(turma), linhas);
    }

    /**
     * Reúne, por aluno, as matrículas da turma vigentes no período e os registros salvos.
     * Alunos excluídos aparecem somente quando possuem histórico no período, como no diário.
     */
    private List<AlunoDaTurma> carregarAlunos(Turma turma, LocalDate inicio, LocalDate fim, List<Ocorrencia> ocorrencias) {
        List<Matricula> matriculas = matriculaRepository.findVigentesPorTurmaNoPeriodo(
                turma.getId(), fim.plusDays(1).atStartOfDay(), inicio);
        List<Frequencia> frequencias = frequenciaRepository.findPorTurmaNoPeriodo(turma.getId(), inicio, fim);

        Map<Integer, AlunoDaTurma> alunos = new LinkedHashMap<>();
        for (Matricula matricula : matriculas) {
            alunos.computeIfAbsent(matricula.getUsuario().getId(), id -> new AlunoDaTurma(matricula.getUsuario()))
                    .matriculas.add(matricula);
        }
        for (Frequencia frequencia : frequencias) {
            AlunoDaTurma aluno = alunos.computeIfAbsent(frequencia.getMatricula().getUsuario().getId(),
                    id -> new AlunoDaTurma(frequencia.getMatricula().getUsuario()));
            aluno.possuiHistorico = true;
            aluno.registrar(frequencia);
        }
        for (Ocorrencia ocorrencia : ocorrencias) {
            alunos.computeIfAbsent(ocorrencia.getMatricula().getUsuario().getId(),
                    id -> new AlunoDaTurma(ocorrencia.getMatricula().getUsuario())).possuiHistorico = true;
        }

        if (!alunos.isEmpty()) {
            Set<Integer> excluidos = new HashSet<>(matriculaRepository.findIdsUsuariosExcluidos(alunos.keySet()));
            alunos.values().forEach(a -> a.excluido = excluidos.contains(a.idUsuario));
        }

        Collator collator = collator();
        return alunos.values().stream()
                .filter(a -> a.possuiHistorico || a.matriculas.stream().anyMatch(m -> m.getStatus() != StatusMatricula.EXCLUIDO))
                .sorted(Comparator.comparing((AlunoDaTurma a) -> a.nome, collator).thenComparing(a -> a.idUsuario))
                .toList();
    }

    private static final class AlunoDaTurma {
        final Integer idUsuario;
        final String nome;
        final List<Matricula> matriculas = new ArrayList<>();
        final Map<LocalDate, Frequencia> frequencias = new LinkedHashMap<>();
        boolean possuiHistorico;
        boolean excluido;

        AlunoDaTurma(Usuario usuario) {
            this.idUsuario = usuario.getId();
            this.nome = usuario.getNomeCompleto() == null ? "" : usuario.getNomeCompleto();
        }

        void registrar(Frequencia frequencia) {
            // Com mais de uma matrícula na mesma data, prevalece o registro da matrícula vigente.
            Frequencia atual = frequencias.get(frequencia.getDataRegistro());
            if (atual == null || (!vigente(atual.getMatricula(), atual.getDataRegistro())
                    && vigente(frequencia.getMatricula(), frequencia.getDataRegistro()))) {
                frequencias.put(frequencia.getDataRegistro(), frequencia);
            }
        }

        Indicador indicador(LocalDate data) {
            boolean matriculado = matriculas.stream().anyMatch(m -> vigente(m, data));
            Frequencia frequencia = frequencias.get(data);
            if (frequencia == null) {
                return matriculado ? Indicador.NAO_REGISTRADO : Indicador.NAO_MATRICULADO;
            }
            boolean presente = Boolean.TRUE.equals(frequencia.getPresente());
            if (matriculado && vigente(frequencia.getMatricula(), data)) {
                return presente ? Indicador.PRESENTE : Indicador.FALTA;
            }
            return presente ? Indicador.PRESENTE_FORA_DA_MATRICULA : Indicador.FALTA_FORA_DA_MATRICULA;
        }

        DadosRelatorio.Aluno dados() {
            return new DadosRelatorio.Aluno(nome, excluido);
        }
    }

    static boolean vigente(Matricula matricula, LocalDate data) {
        return matricula.getDataIngresso() != null
                && !matricula.getDataIngresso().toLocalDate().isAfter(data)
                && (matricula.getDataDesligamento() == null || !matricula.getDataDesligamento().isBefore(data));
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (fim.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data final não pode ser anterior à data inicial.");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) + 1 > LIMITE_DIAS_PERIODO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O período máximo é de " + LIMITE_DIAS_PERIODO + " dias. Divida a exportação em períodos menores.");
        }
    }

    private Turma buscarTurmaComAcesso(Integer id, Membro membro) {
        Turma turma = turmaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada."));
        Unidade unidade = turma.getUnidade();
        if (Perfil.fromNomePapel(membro.getPapel().getNomePapel()) != Perfil.ADMINISTRADOR
                && membro.getUnidades().stream().noneMatch(u -> u.getId().equals(unidade.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "O usuário não possui permissão de acesso a esta unidade.");
        }
        return turma;
    }

    @FunctionalInterface
    interface Gerador {
        byte[] gerar() throws IOException;
    }

    private ArquivoRelatorio gerarArquivo(String nome, FormatoRelatorio formato, Gerador pdf, Gerador xlsx) {
        try {
            return formato == FormatoRelatorio.PDF
                    ? new ArquivoRelatorio(nome + ".pdf", TIPO_PDF, pdf.gerar())
                    : new ArquivoRelatorio(nome + ".xlsx", TIPO_XLSX, xlsx.gerar());
        } catch (IOException | RuntimeException exception) {
            log.error("Falha ao gerar o relatório {}", nome, exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível gerar o relatório. Tente novamente.", exception);
        }
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(FUSO_BRASILIA).withNano(0);
    }

    static Collator collator() {
        Collator collator = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        collator.setStrength(Collator.SECONDARY);
        return collator;
    }

    static DadosRelatorio.Turma descricao(Turma turma) {
        return new DadosRelatorio.Turma(turma.getUnidade().getNome(),
                periodo(turma.getPeriodo()) + " (" + turma.getHoraInicio().format(HORA) + " – "
                        + turma.getHoraFim().format(HORA) + ")", periodo(turma.getPeriodo()));
    }

    private static String periodo(Periodo periodo) {
        return periodo == Periodo.MANHA ? "Manhã" : "Tarde";
    }

    private static String rotulo(TipoOcorrencia tipo) {
        return switch (tipo) {
            case COMPORTAMENTO -> "Comportamento";
            case SAUDE -> "Saúde";
            case ASSISTENCIA -> "Assistência";
            case OUTRO -> "Outro";
        };
    }

    static String slug(String texto) {
        String semAcento = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.length() > 40 ? slug.substring(0, 40).replaceAll("-$", "") : slug;
    }
}
