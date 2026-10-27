package br.org.larescolaredencao.service;

import br.org.larescolaredencao.audit.EntidadeAuditada;
import br.org.larescolaredencao.audit.ComparadorAuditoria;
import br.org.larescolaredencao.dto.FiltroHistoricoDTO;
import br.org.larescolaredencao.dto.HistoricoDetalheDTO;
import br.org.larescolaredencao.dto.HistoricoDetalheDTO.CampoAlteradoDTO;
import br.org.larescolaredencao.dto.HistoricoEventoDTO;
import br.org.larescolaredencao.model.enums.TipoAlteracaoHistorico;
import br.org.larescolaredencao.model.enums.TipoRegistroHistorico;
import br.org.larescolaredencao.repository.HistoricoRepository;
import br.org.larescolaredencao.repository.HistoricoRepository.Linha;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class HistoricoService {

    private static final int TAMANHO_MAXIMO_PAGINA = 100;
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Map<String, String> NOMES_CAMPOS = criarNomesCampos();

    private final HistoricoRepository historicoRepository;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public HistoricoService(HistoricoRepository historicoRepository) {
        this.historicoRepository = historicoRepository;
    }

    public Page<HistoricoEventoDTO> listar(FiltroHistoricoDTO filtro, Pageable pageable) {
        validar(filtro);
        validarPaginacao(pageable);

        PageRequest paginacao = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return historicoRepository.listar(filtro, paginacao).map(this::montarResumo);
    }

    public HistoricoDetalheDTO detalhar(long id) {
        if (id < 1) {
            throw parametroInvalido("O ID do evento deve ser positivo.");
        }

        Linha evento = historicoRepository.buscar(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Evento do histórico não encontrado."));

        return montarDetalhe(evento);
    }

    HistoricoDetalheDTO montarDetalhe(Linha evento) {

        Map<String, Object> valoresAnteriores = lerValores(evento.anterior());
        Map<String, Object> valoresNovos = lerValores(evento.novo());
        boolean referenciaAnteriorDisponivel = evento.anterior() != null
                || TipoAlteracaoHistorico.CADASTRO.name().equals(evento.tipoAlteracao());

        String observacao = null;
        if (!referenciaAnteriorDisponivel) {
            observacao = "Referência anterior indisponível; os valores desconhecidos não representam campos vazios.";
        }

        List<CampoAlteradoDTO> campos = montarCamposAlterados(
                evento, valoresAnteriores, valoresNovos, referenciaAnteriorDisponivel);

        return new HistoricoDetalheDTO(
                montarResumo(evento), evento.chave(), referenciaAnteriorDisponivel, observacao,
                lerValores(evento.contextoAnterior()), lerValores(evento.contextoNovo()), campos);
    }

    public static void validar(FiltroHistoricoDTO filtro) {
        if (filtro.tipoRegistro() == null) {
            throw parametroInvalido("Informe o tipo de registro.");
        }

        boolean registroInvalido = filtro.idRegistro() != null && filtro.idRegistro() < 1;
        boolean responsavelInvalido = filtro.idResponsavel() != null && filtro.idResponsavel() < 1;
        if (registroInvalido || responsavelInvalido) {
            throw parametroInvalido("Os IDs devem ser positivos.");
        }

        if (filtro.dataInicial() != null && filtro.dataFinal() != null
                && filtro.dataFinal().isBefore(filtro.dataInicial())) {
            throw parametroInvalido("A data final não pode ser anterior à data inicial.");
        }

        if (LocalDate.MAX.equals(filtro.dataFinal())) {
            throw parametroInvalido("Data final fora do intervalo permitido.");
        }

        boolean presencas = filtro.tipoRegistro() == TipoRegistroHistorico.PRESENCAS;
        if (presencas && filtro.idRegistro() != null) {
            throw parametroInvalido("Presenças não permitem filtro pelo ID da frequência.");
        }

        if (!presencas && filtro.dataAula() != null) {
            throw parametroInvalido("Data da aula está disponível somente para Presenças.");
        }
    }

    private void validarPaginacao(Pageable pageable) {
        if (pageable.getPageSize() > TAMANHO_MAXIMO_PAGINA) {
            throw parametroInvalido("O tamanho máximo da página é 100.");
        }

        if (pageable.getSort().isSorted()) {
            throw parametroInvalido("O histórico utiliza ordenação fixa por eventos mais recentes.");
        }
    }

    private List<CampoAlteradoDTO> montarCamposAlterados(
            Linha evento,
            Map<String, Object> valoresAnteriores,
            Map<String, Object> valoresNovos,
            boolean referenciaAnteriorDisponivel) {

        Set<String> nomesCampos = new LinkedHashSet<>(valoresAnteriores.keySet());
        nomesCampos.addAll(valoresNovos.keySet());

        List<CampoAlteradoDTO> camposAlterados = new ArrayList<>();
        boolean edicao = TipoAlteracaoHistorico.EDICAO.name().equals(evento.tipoAlteracao());

        for (String campo : nomesCampos) {
            Object valorAnterior = valoresAnteriores.get(campo);
            Object valorNovo = valoresNovos.get(campo);
            if (edicao && referenciaAnteriorDisponivel && ComparadorAuditoria.equivalentes(valorAnterior, valorNovo)) {
                continue;
            }

            String nome = NOMES_CAMPOS.getOrDefault(campo, formatarNomeCampo(campo));
            camposAlterados.add(new CampoAlteradoDTO(
                    campo, nome, valorAnterior, valorNovo, referenciaAnteriorDisponivel));
        }

        return camposAlterados;
    }

    HistoricoEventoDTO montarResumo(Linha evento) {
        TipoRegistroHistorico categoria = TipoRegistroHistorico.valueOf(evento.categoria());
        TipoAlteracaoHistorico tipoAlteracao = TipoAlteracaoHistorico.valueOf(evento.tipoAlteracao());
        Map<String, Object> valores = lerValores(evento.novo() != null ? evento.novo() : evento.anterior());
        Map<String, Object> contexto = lerValores(
                evento.novo() != null ? evento.contextoNovo() : evento.contextoAnterior());

        String identificacao = montarIdentificacao(evento, categoria, valores, contexto);
        String informacaoAlterada = Arrays.stream(EntidadeAuditada.values())
                .filter(entidade -> entidade.tabela.equals(evento.entidade()))
                .map(entidade -> entidade.descricao)
                .findFirst()
                .orElse("parceiro_evento".equals(evento.entidade()) ? "Parceiros do evento" : "Vínculos de unidades");
        if ("secao".equals(evento.entidade())) {
            informacaoAlterada = switch (Objects.toString(valores.get("grupo"), "")) {
                case "historia" -> "Marco histórico";
                case "carrossel" -> "Imagem do carrossel";
                case "texto-sobre" -> "Texto sobre a OSC";
                case "produtos" -> "Produto ou serviço";
                case "telefone" -> "Contato da gráfica";
                case "pix" -> "Dados para doação";
                default -> informacaoAlterada;
            };
        }

        OffsetDateTime dataHora = Instant.ofEpochMilli(evento.instante())
                .atZone(HistoricoRepository.FUSO)
                .toOffsetDateTime();
        boolean registroExcluido = tipoAlteracao == TipoAlteracaoHistorico.EXCLUSAO
                || "EXCLUIDO".equals(valores.get("status"));

        return new HistoricoEventoDTO(
                evento.id(), evento.revisao(), dataHora,
                evento.idResponsavel(), evento.nomeResponsavel(),
                categoria, informacaoAlterada, evento.idRegistro(),
                identificacao, registroExcluido, tipoAlteracao);
    }

    private String montarIdentificacao(
            Linha evento,
            TipoRegistroHistorico categoria,
            Map<String, Object> valores,
            Map<String, Object> contexto) {

        if (categoria == TipoRegistroHistorico.UNIDADES) {
            return Objects.toString(valores.get("nome"), "Unidade");
        }
        if (categoria == TipoRegistroHistorico.TURMAS) {
            return obterPeriodo(valores) + ", " + obterTitulo(contexto.get("unidade"), "Unidade");
        }
        if (categoria == TipoRegistroHistorico.EVENTOS) {
            return Objects.toString(valores.get("titulo"), Objects.toString(contexto.get("titulo"),
                    obterTitulo(contexto.get("evento"), "Evento")));
        }
        if (categoria == TipoRegistroHistorico.DIRETORIA || categoria == TipoRegistroHistorico.PARCEIROS
                || categoria == TipoRegistroHistorico.REDES_SOCIAIS) {
            return Objects.toString(valores.get("nome"), "Registro");
        }
        if (List.of(TipoRegistroHistorico.TRANSPARENCIA, TipoRegistroHistorico.CONHECA_OSC,
                TipoRegistroHistorico.GRAFICA, TipoRegistroHistorico.PIX).contains(categoria)) {
            String pagina = obterTitulo(contexto.get("pagina"), "Página");
            if (categoria == TipoRegistroHistorico.CONHECA_OSC) pagina = "Conheça a OSC";
            String grupo = Objects.toString(valores.get("grupo"), "");
            if (categoria == TipoRegistroHistorico.PIX) return "Doações via Pix";
            if (categoria == TipoRegistroHistorico.GRAFICA && grupo.equals("telefone")) return "Contato da gráfica";
            if (categoria == TipoRegistroHistorico.CONHECA_OSC) {
                if (grupo.equals("historia")) return "Nossa história, " + valores.get("titulo");
                if (grupo.equals("carrossel")) return "Imagem do carrossel";
                if (grupo.equals("texto-sobre")) return "Texto sobre a OSC";
            }
            return Objects.toString(valores.get("titulo"), Objects.toString(valores.get("nome"), pagina));
        }
        String nomeContexto = Objects.toString(contexto.get("nomeCompleto"), "Registro");
        String identificacao = Objects.toString(valores.get("nomeCompleto"), nomeContexto);

        if (categoria == TipoRegistroHistorico.USUARIOS
                || categoria == TipoRegistroHistorico.PRESENCAS
                || categoria == TipoRegistroHistorico.OCORRENCIAS) {
            identificacao = obterNome(contexto.get("usuario"), identificacao);
        }

        if (categoria == TipoRegistroHistorico.VINCULOS_CONTATOS) {
            String nomeContato = obterNome(contexto.get("contato"), "Contato");
            String nomeUsuario = obterNome(contexto.get("usuario"), "Usuário");
            identificacao = nomeContato + ", contato de " + nomeUsuario;
        }

        if (categoria == TipoRegistroHistorico.PRESENCAS) {
            String dataAula = Objects.toString(valores.get("dataRegistro"), "");
            if (!dataAula.isEmpty()) {
                dataAula = LocalDate.parse(dataAula).format(FORMATO_DATA);
            }
            return identificacao + ", " + obterPeriodo(contexto.get("turma")) + ", " + dataAula;
        }

        return identificacao;
    }

    private String obterTitulo(Object referencia, String padrao) {
        if (referencia instanceof Map<?, ?> valores) {
            return Objects.toString(valores.get("titulo"), Objects.toString(valores.get("nome"), padrao));
        }
        return padrao;
    }

    private String obterNome(Object referencia, String nomePadrao) {
        if (!(referencia instanceof Map<?, ?> valores)) {
            return nomePadrao;
        }

        String nomeBanco = Objects.toString(valores.get("nome_completo"), nomePadrao);
        return Objects.toString(valores.get("nomeCompleto"), nomeBanco);
    }

    private String obterPeriodo(Object referencia) {
        if (!(referencia instanceof Map<?, ?> turma)) {
            return "Turma";
        }

        String periodo = Objects.toString(turma.get("periodo"), "Turma");
        return switch (periodo) {
            case "MANHA" -> "Manhã";
            case "TARDE" -> "Tarde";
            default -> periodo;
        };
    }

    private Map<String, Object> lerValores(String valores) {
        if (valores == null) {
            return Map.of();
        }
        return jsonMapper.readValue(valores, new TypeReference<LinkedHashMap<String, Object>>() {});
    }

    private static ResponseStatusException parametroInvalido(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }

    private static String formatarNomeCampo(String campo) {
        String nome = campo.replaceAll("([a-z])([A-Z])", "$1 $2");
        return nome.substring(0, 1).toUpperCase() + nome.substring(1).toLowerCase(Locale.ROOT);
    }

    private static Map<String, String> criarNomesCampos() {
        Map<String, String> nomes = new HashMap<>();
        nomes.put("id", "ID do registro");
        nomes.put("idUsuario", "ID do usuário");
        nomes.put("idContato", "ID do contato");
        nomes.put("nomeCompleto", "Nome completo");
        nomes.put("cpf", "CPF");
        nomes.put("email", "E-mail");
        nomes.put("endereco", "Endereço");
        nomes.put("telefone", "Telefone");
        nomes.put("usuario", "ID do usuário");
        nomes.put("contato", "ID do contato");
        nomes.put("turma", "ID da turma");
        nomes.put("matricula", "ID da matrícula");
        nomes.put("membro", "ID do colaborador do registro");
        nomes.put("papel", "ID do perfil");
        nomes.put("unidades", "Unidades de atuação");
        nomes.put("unidade", "Unidade");
        nomes.put("periodo", "Período");
        nomes.put("horaInicio", "Horário de início");
        nomes.put("horaFim", "Horário de término");
        nomes.put("horarioAbertura", "Horário de abertura");
        nomes.put("horarioFechamento", "Horário de fechamento");
        nomes.put("diasFuncionamento", "Dias de funcionamento");
        nomes.put("idadeMin", "Idade mínima");
        nomes.put("idadeMax", "Idade máxima");
        nomes.put("corHex", "Cor de identificação");
        nomes.put("dataRegistro", "Data da aula");
        nomes.put("dataCriacao", "Data de criação");
        nomes.put("dataOcorrencia", "Data e hora do fato");
        nomes.put("dataIngresso", "Data de ingresso");
        nomes.put("dataDesligamento", "Data de desligamento");
        nomes.put("presente", "Presença");
        nomes.put("descricao", "Descrição");
        nomes.put("tipoOcorrencia", "Tipo de ocorrência");
        nomes.put("caminhoArquivo", "Referência do arquivo");
        nomes.put("dataUpload", "Data do envio");
        nomes.put("parentescoVinculo", "Parentesco ou vínculo");
        nomes.put("composicaoFamiliar", "Composição familiar");
        nomes.put("dataEvento", "Data e hora do evento");
        nomes.put("tipoEvento", "Categoria do evento");
        nomes.put("ativo", "Exibição no site");
        nomes.put("url", "Endereço do link");
        nomes.put("urlLink", "Link da publicação");
        nomes.put("icone", "Ícone");
        nomes.put("logo", "Logo");
        nomes.put("conteudo", "Conteúdo");
        nomes.put("titulo", "Título");
        nomes.put("secao", "Seção");
        nomes.put("pagina", "Página");
        nomes.put("redeSocial", "Rede social");
        nomes.put("parceiros", "Parceiros do evento");
        return Map.copyOf(nomes);
    }
}
