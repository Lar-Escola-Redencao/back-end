package br.org.larescolaredencao.service.relatorio;

import br.org.larescolaredencao.model.EntrevistaSocial;
import br.org.larescolaredencao.model.Frequencia;
import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Ocorrencia;
import br.org.larescolaredencao.model.Unidade;
import br.org.larescolaredencao.model.enums.Perfil;
import br.org.larescolaredencao.model.enums.FormatoRelatorio;
import br.org.larescolaredencao.model.enums.Periodo;
import br.org.larescolaredencao.model.enums.TipoOcorrencia;
import br.org.larescolaredencao.repository.EntrevistaSocialRepository;
import br.org.larescolaredencao.repository.FrequenciaRepository;
import br.org.larescolaredencao.repository.MembroRepository;
import br.org.larescolaredencao.repository.OcorrenciaRepository;
import br.org.larescolaredencao.service.FrequenciaAcompanhamentoService;
import br.org.larescolaredencao.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RelatorioAcompanhamentoService {
    private static final Logger log = LoggerFactory.getLogger(RelatorioAcompanhamentoService.class);

    private final UsuarioService usuarios;
    private final FrequenciaAcompanhamentoService selecao;
    private final FrequenciaRepository frequencias;
    private final OcorrenciaRepository ocorrencias;
    private final EntrevistaSocialRepository entrevistas;
    private final MembroRepository membros;
    private final RelatorioAcompanhamentoPdfGerador pdf;
    private final RelatorioXlsxGerador xlsx;
    private final Clock relogio;

    @Autowired
    public RelatorioAcompanhamentoService(UsuarioService usuarios, FrequenciaAcompanhamentoService selecao,
            FrequenciaRepository frequencias, OcorrenciaRepository ocorrencias,
            EntrevistaSocialRepository entrevistas, MembroRepository membros, RelatorioAcompanhamentoPdfGerador pdf, RelatorioXlsxGerador xlsx) {
        this(usuarios, selecao, frequencias, ocorrencias, entrevistas, membros, pdf, xlsx,
                Clock.system(ZoneId.of("America/Sao_Paulo")));
    }

    RelatorioAcompanhamentoService(UsuarioService usuarios, FrequenciaAcompanhamentoService selecao,
            FrequenciaRepository frequencias, OcorrenciaRepository ocorrencias,
            EntrevistaSocialRepository entrevistas, MembroRepository membros, RelatorioAcompanhamentoPdfGerador pdf,
            RelatorioXlsxGerador xlsx, Clock relogio) {
        this.usuarios = usuarios; this.selecao = selecao; this.frequencias = frequencias;
        this.ocorrencias = ocorrencias; this.entrevistas = entrevistas; this.membros = membros;
        this.pdf = pdf; this.xlsx = xlsx; this.relogio = relogio;
    }

    public ArquivoRelatorio gerar(Integer usuario, Integer matricula, LocalDate inicio, LocalDate fim, Membro membro) {
        return gerar(usuario, matricula, inicio, fim, membro, FormatoRelatorio.PDF);
    }

    public ArquivoRelatorio gerar(Integer usuario, Integer matricula, LocalDate inicio, LocalDate fim,
            Membro membro, FormatoRelatorio formato) {
        DadosAcompanhamento dados = montar(usuario, matricula, inicio, fim, membro);
        String nome = "acompanhamento_" + RelatorioFrequenciaService.slug(dados.usuario()) + "_"
                + dados.geradoEm().toLocalDate();
        try {
            return formato == FormatoRelatorio.XLSX
                    ? new ArquivoRelatorio(nome + ".xlsx", RelatorioFrequenciaService.TIPO_XLSX, xlsx.acompanhamento(dados))
                    : new ArquivoRelatorio(nome + ".pdf", RelatorioFrequenciaService.TIPO_PDF, pdf.gerar(dados));
        } catch (java.io.IOException | RuntimeException e) {
            log.error("Falha ao gerar o relatório de acompanhamento do usuário {}", usuario, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível gerar o relatório. Tente novamente.", e);
        }
    }

    DadosAcompanhamento montar(Integer usuario, Integer matricula, LocalDate inicio, LocalDate fim, Membro membro) {
        var nomeUsuario = usuarios.buscarUsuarioPorId(usuario, membro).getNomeCompleto();
        var escolha = selecao.selecionar(usuario, matricula, inicio, fim, membro);
        inicio = escolha.intervalo().inicio(); fim = escolha.intervalo().fim();
        var periodo = escolha.intervalo();
        List<Matricula> alvo = escolha.matriculas().stream()
                .filter(m -> m.getDataIngresso() != null && !m.getDataIngresso().toLocalDate().isAfter(periodo.fim())
                        && (m.getDataDesligamento() == null || !m.getDataDesligamento().isBefore(periodo.inicio())))
                .toList();
        if (matricula != null && alvo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O período selecionado está fora da vigência da matrícula. Escolha um período entre o ingresso e o desligamento.");
        }
        List<Integer> ids = alvo.stream().map(Matricula::getId).toList();
        var fimExclusivo = fim.plusDays(1).atStartOfDay();

        Map<LocalDate, List<DadosAcompanhamento.RegistroDia>> registros = new LinkedHashMap<>();
        List<DadosAcompanhamento.Ocorrencia> listaOcorrencias = List.of();
        if (!ids.isEmpty()) {
            for (Frequencia f : frequencias.findPorMatriculasNoPeriodo(ids, inicio, fim)) {
                registros.computeIfAbsent(f.getDataRegistro(), d -> new ArrayList<>())
                        .add(new DadosAcompanhamento.RegistroDia(f.getMatricula().getId(),
                                Boolean.TRUE.equals(f.getPresente())));
            }
        }
        boolean administrador = Perfil.fromNomePapel(membro.getPapel().getNomePapel()) == Perfil.ADMINISTRADOR;
        var unidadesPermitidas = membro.getUnidades().stream().map(Unidade::getId).toList();
        var idsOcorrencias = alvo.stream()
                .filter(m -> administrador || unidadesPermitidas.contains(m.getTurma().getUnidade().getId()))
                .map(Matricula::getId).toList();
        if (!idsOcorrencias.isEmpty()) {
            listaOcorrencias = ocorrencias.listarParaRelatorioAcompanhamento(idsOcorrencias, inicio.atStartOfDay(), fimExclusivo)
                    .stream().map(RelatorioAcompanhamentoService::ocorrencia).toList();
        }

        List<DadosAcompanhamento.Entrevista> listaEntrevistas = null;
        Perfil perfil = Perfil.fromNomePapel(membro.getPapel().getNomePapel());
        if (perfil == Perfil.ADMINISTRADOR || perfil == Perfil.COORDENADOR) {
            var lista = entrevistas.listarParaRelatorio(usuario, inicio.atStartOfDay(), fimExclusivo);
            var autores = membros.findAllById(lista.stream().map(EntrevistaSocial::getIdMembro).distinct().toList())
                    .stream().collect(Collectors.toMap(Membro::getId, Membro::getNomeCompleto));
            listaEntrevistas = lista.stream().map(e -> new DadosAcompanhamento.Entrevista(
                    e.getDataEntrevista(), e.getRelato(), autores.getOrDefault(e.getIdMembro(), "-"))).toList();
        }

        List<DadosAcompanhamento.MatriculaRelatorio> infos = alvo.stream().map(m -> new DadosAcompanhamento.MatriculaRelatorio(
                m.getId(), m.getTurma().getUnidade().getNome(), turma(m),
                m.getDataIngresso() == null ? null : m.getDataIngresso().toLocalDate(), m.getDataDesligamento()))
                .sorted(java.util.Comparator.comparingInt(DadosAcompanhamento.MatriculaRelatorio::id)).toList();
        String filtro = matricula == null ? "Todas as matrículas"
                : "Matrícula " + matricula + " — " + infos.get(0).unidade() + ", " + infos.get(0).turma();
        return new DadosAcompanhamento(nomeUsuario, inicio, fim, filtro,
                LocalDateTime.now(relogio).withNano(0), membro.getNomeCompleto(), infos, registros,
                listaOcorrencias, listaEntrevistas);
    }

    private static DadosAcompanhamento.Ocorrencia ocorrencia(Ocorrencia o) {
        return new DadosAcompanhamento.Ocorrencia(o.getMatricula().getId(), o.getDataOcorrencia(), rotulo(o.getTipoOcorrencia()),
                o.getDescricao(), o.getMembro().getNomeCompleto(),
                o.getMatricula().getTurma().getUnidade().getNome() + " — " + turma(o.getMatricula()));
    }

    private static String turma(Matricula m) {
        return m.getTurma().getPeriodo() == Periodo.MANHA ? "Manhã" : "Tarde";
    }

    private static String rotulo(TipoOcorrencia tipo) {
        return switch (tipo) {
            case COMPORTAMENTO -> "Comportamento";
            case SAUDE -> "Saúde";
            case ASSISTENCIA -> "Assistência";
            case OUTRO -> "Outro";
        };
    }
}
