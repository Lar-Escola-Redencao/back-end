package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;
import br.org.larescolaredencao.dto.FiltroHistoricoDTO;
import br.org.larescolaredencao.model.enums.TipoRegistroHistorico;
import br.org.larescolaredencao.repository.HistoricoRepository;
import br.org.larescolaredencao.repository.HistoricoRepository.Linha;

class HistoricoServiceTest {

    @Test
    void identificaEntrevistaSeparadamenteMesmoEmEventoAntigoClassificadoComoUsuario() {
        var linha = new Linha(41, 2, 0L, 9, "Ana", "USUARIOS", "entrevista_social", "7", 12,
                "EDICAO", "{\"relato\":\"Antes\"}", "{\"relato\":\"Depois\"}",
                "{\"idUsuario\":12,\"usuario\":{\"nomeCompleto\":\"Arthur\"}}",
                "{\"idUsuario\":12,\"usuario\":{\"nomeCompleto\":\"Arthur\"}}");
        var resumo = new HistoricoService(mock(HistoricoRepository.class)).montarResumo(linha);
        assertThat(resumo.tipoRegistro().name()).isEqualTo("ENTREVISTAS");
        assertThat(resumo.identificacaoRegistro()).isEqualTo("Arthur");
        assertThat(resumo.informacaoAlterada()).isEqualTo("Entrevista social");
    }

    @Test
    void identificaContatoEVinculoComVirgulaPreservandoApelido() {
        String contexto = """
                {"contato":{"nomeCompleto":"João Batista (Seu João)"},"usuario":{"nomeCompleto":"Arthur Silva Gomes"}}
                """;
        var linha = new Linha(40, 2, 0L, 9, "Ana", "VINCULOS_CONTATOS", "contato_usuario", "7", 7,
                "EDICAO", "{}", "{}", contexto, contexto);
        var service = new HistoricoService(mock(HistoricoRepository.class));
        assertThat(service.montarResumo(linha).identificacaoRegistro())
                .isEqualTo("João Batista (Seu João), contato de Arthur Silva Gomes");
    }

    private final HistoricoRepository historicoRepository = mock(HistoricoRepository.class);
    private final HistoricoService historicoService = new HistoricoService(historicoRepository);

    @Test
    void rejeitaIdDaFrequenciaAntesDeConsultarBanco() {
        var filtro = filtro(TipoRegistroHistorico.PRESENCAS, 12, null, null, null);
        assertThatThrownBy(() -> historicoService.listar(filtro, PageRequest.of(0, 20)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("ID da frequência");
        verifyNoInteractions(historicoRepository);
    }

    @Test
    void rejeitaPeriodoInvertidoEDataDeAulaEmOutraCategoria() {
        assertThatThrownBy(() -> HistoricoService.validar(filtro(TipoRegistroHistorico.USUARIOS, null,
                LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1), null)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("data final");
        assertThatThrownBy(() -> HistoricoService.validar(filtro(TipoRegistroHistorico.CONTATOS, null,
                null, null, LocalDate.of(2026, 9, 1))))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("somente para Presenças");
    }

    @Test
    void detalhesRetemNomeHistoricoEExibemApenasCamposAlterados() {
        when(historicoRepository.buscar(31)).thenReturn(Optional.of(linha("EDICAO",
                "{\"id\":7,\"nomeCompleto\":\"Ana Antiga\",\"email\":null}",
                "{\"id\":7,\"nomeCompleto\":\"Ana Nova\",\"email\":null}")));
        var detalhe = historicoService.detalhar(31);
        assertThat(detalhe.referenciaAnteriorDisponivel()).isTrue();
        assertThat(detalhe.evento().nomeResponsavel()).isEqualTo("Responsável histórico");
        assertThat(detalhe.campos()).singleElement().satisfies(campo -> {
            assertThat(campo.campo()).isEqualTo("nomeCompleto");
            assertThat(campo.valorAnterior()).isEqualTo("Ana Antiga");
            assertThat(campo.valorNovo()).isEqualTo("Ana Nova");
            assertThat(campo.anteriorConhecido()).isTrue();
        });
    }

    @Test
    void edicaoDoTituloIgnoraRepresentacoesEquivalentesDoValor() {
        var evento = eventoEditado(
                """
                {"id":2,"titulo":"Festival","valor":0.0}
                """,
                """
                {"id":2,"titulo":"Festival revisado","valor":0}
                """);

        var detalhe = historicoService.montarDetalhe(evento);

        assertThat(detalhe.campos()).singleElement().satisfies(campo -> {
            assertThat(campo.campo()).isEqualTo("titulo");
            assertThat(campo.valorAnterior()).isEqualTo("Festival");
            assertThat(campo.valorNovo()).isEqualTo("Festival revisado");
        });
    }

    @Test
    void diferenciaMudancaRealDeValorDeDiferencaNasCasasDecimais() {
        var semMudanca = eventoEditado("{\"valor\":10.50}", "{\"valor\":10.5}");
        var comMudanca = eventoEditado("{\"valor\":10.50}", "{\"valor\":10.51}");

        assertThat(historicoService.montarDetalhe(semMudanca).campos()).isEmpty();
        assertThat(historicoService.montarDetalhe(comMudanca).campos()).singleElement().satisfies(campo -> {
            assertThat(campo.campo()).isEqualTo("valor");
            assertThat(campo.valorAnterior()).isEqualTo(10.5);
            assertThat(campo.valorNovo()).isEqualTo(10.51);
        });
    }

    @Test
    void alterarNomeIgnoraCamposQueContinuamVazios() {
        var evento = eventoEditado(
                "{\"nomeCompleto\":\"Arthur Silva Gomes\",\"cep\":null,\"cadUnico\":null,\"raEscolar\":null}",
                "{\"nomeCompleto\":\"Arthur Silva GomesAL\",\"cep\":\"\",\"cadUnico\":\"\",\"raEscolar\":\"\"}");

        assertThat(historicoService.montarDetalhe(evento).campos()).singleElement()
                .satisfies(campo -> assertThat(campo.campo()).isEqualTo("nomeCompleto"));
    }

    @Test
    void limpezaRealDoCpfContinuaNoHistorico() {
        var evento = eventoEditado("{\"cpf\":\"52998224725\"}", "{\"cpf\":null}");

        assertThat(historicoService.montarDetalhe(evento).campos()).singleElement().satisfies(campo -> {
            assertThat(campo.campo()).isEqualTo("cpf");
            assertThat(campo.valorAnterior()).isEqualTo("52998224725");
            assertThat(campo.valorNovo()).isNull();
        });
    }

    @Test
    void valorNuloContinuaDiferenteDeZero() {
        var evento = eventoEditado("{\"valor\":null}", "{\"valor\":0}");

        var detalhe = historicoService.montarDetalhe(evento);

        assertThat(detalhe.campos()).singleElement().satisfies(campo -> {
            assertThat(campo.campo()).isEqualTo("valor");
            assertThat(campo.valorAnterior()).isNull();
            assertThat(campo.valorNovo()).isEqualTo(0);
        });
    }

    @Test
    void diferenciaReferenciaDesconhecidaDeCampoVazio() {
        when(historicoRepository.buscar(31)).thenReturn(Optional.of(linha("EDICAO", null,
                "{\"id\":7,\"email\":null}")));
        var detalhe = historicoService.detalhar(31);
        assertThat(detalhe.referenciaAnteriorDisponivel()).isFalse();
        assertThat(detalhe.observacao()).contains("indisponível");
        assertThat(detalhe.campos()).extracting(campo -> campo.campo()).contains("email");
        assertThat(detalhe.campos()).allSatisfy(campo -> assertThat(campo.anteriorConhecido()).isFalse());
    }

    @Test
    void exclusaoMantemValoresAnterioresSemDependerDoCadastroAtual() {
        when(historicoRepository.buscar(31)).thenReturn(Optional.of(linha("EXCLUSAO",
                "{\"id\":7,\"nomeCompleto\":\"Ana Excluída\"}", null)));
        var detalhe = historicoService.detalhar(31);
        assertThat(detalhe.evento().registroExcluido()).isTrue();
        assertThat(detalhe.evento().identificacaoRegistro()).contains("Ana Excluída");
        assertThat(detalhe.campos()).filteredOn(campo -> campo.campo().equals("nomeCompleto")).singleElement()
                .satisfies(campo -> assertThat(campo.valorAnterior()).isEqualTo("Ana Excluída"));
        verify(historicoRepository).buscar(31);
        verifyNoMoreInteractions(historicoRepository);
    }

    private static Linha linha(String tipo, String anterior, String novo) {
        return new Linha(31, 2, 0L, 9, "Responsável histórico", "CONTATOS", "contato", "7", 7,
                tipo, anterior, novo, "{}", "{}");
    }

    private static Linha eventoEditado(String anterior, String novo) {
        return new Linha(90, 8, 1792500000000L, 1, "Administrador", "EVENTOS", "evento", "2", 2,
                "EDICAO", anterior, novo, "{}", "{}");
    }

    private static FiltroHistoricoDTO filtro(TipoRegistroHistorico tipo, Integer id, LocalDate inicio,
                                            LocalDate fim, LocalDate aula) {
        return new FiltroHistoricoDTO(tipo, null, id, null, inicio, fim, null, aula);
    }

    @Test
    void eventoMantemTituloHistoricoMesmoAposExclusao() {
        var evento = new Linha(90, 8, 1792500000000L, 1, "Administrador", "EVENTOS", "evento", "2", 2,
                "EXCLUSAO", "{\"id\":2,\"titulo\":\"Feira solidária\"}", null, "{}", "{}");

        var resumo = historicoService.montarResumo(evento);

        assertThat(resumo.identificacaoRegistro()).isEqualTo("Feira solidária");
        assertThat(resumo.informacaoAlterada()).isEqualTo("Evento");
        assertThat(resumo.registroExcluido()).isTrue();
    }

    @Test
    void publicacaoIdentificaEventoENaoExibeSuaChaveCompostaComoNome() {
        var evento = new Linha(91, 9, 1792500000000L, 1, "Administrador", "EVENTOS", "evento_rede_social", "2:3", 2,
                "CADASTRO", null, "{\"idEvento\":2,\"idRedeSocial\":3,\"urlLink\":\"https://example.org/noticia\"}",
                "{}", "{\"evento\":{\"titulo\":\"Feira solidária\"},\"redeSocial\":{\"nome\":\"Instagram\"}}");

        assertThat(historicoService.montarResumo(evento).identificacaoRegistro()).isEqualTo("Feira solidária");
    }
    @Test
    void turmaIdentificaPeriodoEUnidadePeloContextoHistorico() {
        var evento = new Linha(92, 10, 1792500000000L, 1, "Administrador", "TURMAS", "turma", "4", 4,
                "EDICAO", "{\"id\":4,\"periodo\":\"MANHA\",\"unidade\":1}",
                "{\"id\":4,\"periodo\":\"TARDE\",\"unidade\":2}",
                "{\"unidade\":{\"nome\":\"Sede\"}}", "{\"unidade\":{\"nome\":\"SOS Bombeiros\"}}");
        when(historicoRepository.buscar(92)).thenReturn(Optional.of(evento));

        var detalhe = historicoService.detalhar(92);

        assertThat(detalhe.evento().identificacaoRegistro()).isEqualTo("Tarde, SOS Bombeiros");
        assertThat(detalhe.campos()).extracting(campo -> campo.nome()).contains("Período", "Unidade");
        assertThat(detalhe.contextoAnterior()).containsEntry("unidade", java.util.Map.of("nome", "Sede"));
        assertThat(detalhe.contextoNovo()).containsEntry("unidade", java.util.Map.of("nome", "SOS Bombeiros"));
    }

    @Test
    void unidadeExcluidaPreservaNomeSemConsultarCadastroAtual() {
        var evento = new Linha(93, 11, 1792500000000L, 1, "Administrador", "UNIDADES", "unidade", "3", 3,
                "EXCLUSAO", "{\"id\":3,\"nome\":\"Unidade Centro\"}", null, "{}", "{}");

        var resumo = historicoService.montarResumo(evento);

        assertThat(resumo.identificacaoRegistro()).isEqualTo("Unidade Centro");
        assertThat(resumo.informacaoAlterada()).isEqualTo("Unidade");
        assertThat(resumo.registroExcluido()).isTrue();
    }
}
