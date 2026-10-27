package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.AtividadeHistoricoDTO;
import br.org.larescolaredencao.dto.EstadoLeituraHistoricoDTO;
import br.org.larescolaredencao.dto.ResponsavelHistoricoDTO;
import br.org.larescolaredencao.dto.ConsultaAtividadesDTO;
import br.org.larescolaredencao.dto.FiltroHistoricoDTO;
import br.org.larescolaredencao.dto.HistoricoEventoDTO;
import br.org.larescolaredencao.model.enums.TipoAlteracaoHistorico;
import br.org.larescolaredencao.model.enums.TipoRegistroHistorico;
import br.org.larescolaredencao.repository.AtividadeHistoricoRepository;
import br.org.larescolaredencao.repository.MembroRepository;
import br.org.larescolaredencao.repository.HistoricoRepository.Linha;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class AtividadeHistoricoService {

    private final AtividadeHistoricoRepository atividadeHistoricoRepository;
    private final HistoricoService historicoService;
    private final MembroRepository membroRepository;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public AtividadeHistoricoService(
            AtividadeHistoricoRepository atividadeHistoricoRepository,
            HistoricoService historicoService,
            MembroRepository membroRepository) {
        this.atividadeHistoricoRepository = atividadeHistoricoRepository;
        this.historicoService = historicoService;
        this.membroRepository = membroRepository;
    }

    public Page<AtividadeHistoricoDTO> listar(ConsultaAtividadesDTO filtro, Pageable pageable) {
        validar(filtro);
        if (pageable.getPageSize() > 50) {
            throw parametroInvalido("Utilize até 50 atividades por página.");
        }
        for (var ordem : pageable.getSort()) {
            if (!List.of("dataHora", "identificacaoRegistro", "nomeResponsavel", "tipoAlteracao")
                    .contains(ordem.getProperty())) {
                throw parametroInvalido("Campo de ordenação inválido para o histórico.");
            }
        }
        return atividadeHistoricoRepository.listar(filtro, pageable)
                .map(linhas -> agrupar(linhas, filtro.idUsuario() != null));
    }

    public EstadoLeituraHistoricoDTO consultarLeitura(ConsultaAtividadesDTO filtro, int administrador) {
        validar(filtro);
        return atividadeHistoricoRepository.consultarLeitura(filtro, administrador);
    }

    @Transactional
    public void registrarLeitura(ConsultaAtividadesDTO filtro, int administrador, long ultimoEvento) {
        validar(filtro);
        long limite = atividadeHistoricoRepository.consultarLeitura(filtro, administrador).ultimoEvento();
        if (ultimoEvento < 0 || ultimoEvento > limite) {
            throw parametroInvalido("A leitura deve corresponder aos eventos disponíveis neste histórico.");
        }
        atividadeHistoricoRepository.registrarLeitura(filtro.contextoLeitura(), administrador, ultimoEvento);
    }

    public List<ResponsavelHistoricoDTO> listarResponsaveis() {
        Map<Integer, ResponsavelHistoricoDTO> responsaveis = new HashMap<>();
        atividadeHistoricoRepository.listarResponsaveis()
                .forEach(responsavel -> responsaveis.put(responsavel.id(), responsavel));
        membroRepository.listarResponsaveisHistorico()
                .forEach(responsavel -> responsaveis.put(responsavel.id(), responsavel));
        return responsaveis.values().stream()
                .sorted(Comparator.comparing(ResponsavelHistoricoDTO::nome, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ResponsavelHistoricoDTO::id))
                .toList();
    }

    AtividadeHistoricoDTO agrupar(List<Linha> linhas) {
        return agrupar(linhas, false);
    }

    private AtividadeHistoricoDTO agrupar(List<Linha> linhas, boolean historicoDoUsuario) {
        List<HistoricoEventoDTO> registros = linhas.stream().map(historicoService::montarResumo).toList();
        HistoricoEventoDTO primeiro = registros.getFirst();
        boolean mesmaAcao = registros.stream().allMatch(evento -> evento.tipoAlteracao() == primeiro.tipoAlteracao());
        TipoAlteracaoHistorico acao = mesmaAcao ? primeiro.tipoAlteracao() : TipoAlteracaoHistorico.EDICAO;
        boolean presencas = primeiro.tipoRegistro() == TipoRegistroHistorico.PRESENCAS;
        String identificacao = presencas ? identificarChamada(linhas.getFirst()) : primeiro.identificacaoRegistro();
        String informacao = presencas ? "Chamada" : primeiro.informacaoAlterada();
        HistoricoEventoDTO evento = new HistoricoEventoDTO(
                primeiro.id(), primeiro.revisao(), primeiro.dataHora(), primeiro.idResponsavel(),
                primeiro.nomeResponsavel(), primeiro.tipoRegistro(), informacao, primeiro.idRegistro(),
                identificacao, primeiro.registroExcluido(), acao);

        String resumo = presencas
                ? resumirChamada(linhas, primeiro.nomeResponsavel(), acao, historicoDoUsuario)
                : resumirRegistro(linhas, registros, primeiro.nomeResponsavel(), acao);
        return new AtividadeHistoricoDTO(evento, resumo, registros);
    }

    private String resumirChamada(List<Linha> linhas, String responsavel, TipoAlteracaoHistorico acao,
                                 boolean historicoDoUsuario) {
        if (historicoDoUsuario) {
            String verbo = switch (acao) {
                case CADASTRO -> "registrou";
                case EDICAO -> "corrigiu";
                case EXCLUSAO -> "excluiu";
            };
            return responsavel + " " + verbo + " a frequência deste aluno.";
        }
        String verbo = switch (acao) {
            case CADASTRO -> "registrou a chamada";
            case EDICAO -> "corrigiu a chamada";
            case EXCLUSAO -> "excluiu registros da chamada";
        };
        long alunos = linhas.stream().map(linha -> contexto(linha).get("idUsuario"))
                .filter(Objects::nonNull).distinct().count();
        return responsavel + " " + verbo + " de " + alunos + (alunos == 1 ? " aluno." : " alunos.");
    }

    private String resumirRegistro(List<Linha> linhas, List<HistoricoEventoDTO> registros,
                                  String responsavel, TipoAlteracaoHistorico acao) {
        String verbo = switch (acao) {
            case CADASTRO -> "cadastrou";
            case EDICAO -> "alterou";
            case EXCLUSAO -> "excluiu";
        };
        String assuntos = registros.stream().map(HistoricoEventoDTO::informacaoAlterada)
                .distinct().map(nome -> nome.toLowerCase(java.util.Locale.ROOT))
                .map(nome -> acao == TipoAlteracaoHistorico.CADASTRO
                        ? nome.replaceFirst("^cadastro d([oa]) ", "$1 ") : nome)
                .collect(java.util.stream.Collectors.joining(", "));
        if (acao == TipoAlteracaoHistorico.EDICAO) {
            List<String> campos = linhas.stream()
                    .flatMap(linha -> historicoService.montarDetalhe(linha).campos().stream())
                    .filter(campo -> !List.of("id", "idUsuario", "idContato").contains(campo.campo()))
                    .map(campo -> campo.nome().replaceFirst("^ID d[oa] ", "").toLowerCase(java.util.Locale.ROOT))
                    .distinct().toList();
            if (!campos.isEmpty()) {
                assuntos = String.join(", ", campos.subList(0, Math.min(3, campos.size())));
                if (campos.size() > 3) {
                    assuntos += " e mais " + (campos.size() - 3) + " campos";
                }
            }
        }
        return responsavel + " " + verbo + " " + assuntos + ".";
    }

    private String identificarChamada(Linha linha) {
        Map<String, Object> contexto = contexto(linha);
        Map<?, ?> turma = contexto.get("turma") instanceof Map<?, ?> valores ? valores : Map.of();
        String periodo = Objects.toString(turma.get("periodo"), "Turma");
        periodo = switch (periodo) {
            case "MANHA" -> "Manhã";
            case "TARDE" -> "Tarde";
            default -> periodo;
        };
        String unidade = Objects.toString(turma.get("unidade"), "");
        Map<String, Object> dados = ler(linha.novo() == null ? linha.anterior() : linha.novo());
        String data = Objects.toString(dados.get("dataRegistro"), "");
        if (!data.isBlank()) {
            data = java.time.LocalDate.parse(data).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        return periodo + (unidade.isBlank() ? "" : " (" + unidade + ")") + ", " + data;
    }

    private Map<String, Object> contexto(Linha linha) {
        return ler(linha.novo() == null ? linha.contextoAnterior() : linha.contextoNovo());
    }

    private Map<String, Object> ler(String json) {
        return json == null ? Map.of() : jsonMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }

    private void validar(ConsultaAtividadesDTO filtro) {
        if (filtro.areaSistema() != null && filtro.tipoRegistro() != null
                && !filtro.areaSistema().tipos().contains(filtro.tipoRegistro())) {
            throw parametroInvalido("O tipo de registro não pertence à área do sistema selecionada.");
        }
        TipoRegistroHistorico categoria = filtro.tipoRegistro() == null
                ? (filtro.dataAula() == null ? TipoRegistroHistorico.USUARIOS : TipoRegistroHistorico.PRESENCAS)
                : filtro.tipoRegistro();
        HistoricoService.validar(new FiltroHistoricoDTO(categoria, filtro.search(), filtro.idRegistro(),
                filtro.idResponsavel(), filtro.dataInicial(), filtro.dataFinal(), filtro.tipoAlteracao(), filtro.dataAula()));
        if (filtro.idRegistro() != null && filtro.tipoRegistro() == null) {
            throw parametroInvalido("Informe a área para consultar um registro específico.");
        }
    }

    private ResponseStatusException parametroInvalido(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
