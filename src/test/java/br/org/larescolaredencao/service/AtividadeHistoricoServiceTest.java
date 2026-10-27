package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.ConsultaAtividadesDTO;
import br.org.larescolaredencao.dto.EstadoLeituraHistoricoDTO;
import br.org.larescolaredencao.dto.ResponsavelHistoricoDTO;
import br.org.larescolaredencao.model.enums.PeriodoHistorico;
import br.org.larescolaredencao.model.enums.AreaSistemaHistorico;
import br.org.larescolaredencao.model.enums.TipoRegistroHistorico;
import br.org.larescolaredencao.repository.AtividadeHistoricoRepository;
import br.org.larescolaredencao.repository.HistoricoRepository;
import br.org.larescolaredencao.repository.MembroRepository;
import br.org.larescolaredencao.repository.HistoricoRepository.Linha;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AtividadeHistoricoServiceTest {

    private final AtividadeHistoricoRepository repository = mock(AtividadeHistoricoRepository.class);
    private final HistoricoService historico = new HistoricoService(mock(HistoricoRepository.class));
    private final MembroRepository membros = mock(MembroRepository.class);
    private final AtividadeHistoricoService service = new AtividadeHistoricoService(repository, historico, membros);

    @Test
    void historicoDoAlunoDescreveSuaFrequenciaSemContarAlunosDaChamada() {
        var filtro = new ConsultaAtividadesDTO(null, null, null, null, null, null,
                null, null, 10, null, null, PeriodoHistorico.TODOS, null);
        var pagina = PageRequest.of(0, 10);
        var verbos = java.util.Map.of("CADASTRO", "registrou", "EDICAO", "corrigiu", "EXCLUSAO", "excluiu");
        for (var acao : verbos.entrySet()) {
            when(repository.listar(filtro, pagina)).thenReturn(new PageImpl<>(
                    List.of(List.of(presenca(1, 10, acao.getKey()))), pagina, 1));

            assertThat(service.listar(filtro, pagina).getContent()).singleElement().satisfies(atividade ->
                    assertThat(atividade.resumo()).isEqualTo("Ana " + acao.getValue() + " a frequência deste aluno."));
        }
    }

    @Test
    void cadastroDoUsuarioNaoRepeteAPalavraCadastroNoResumo() {
        var linha = new Linha(100, 9, 0, 7, "Ana", "USUARIOS", "usuario", "12", 12,
                "CADASTRO", null, "{\"nomeCompleto\":\"Maria\"}", "{}", "{}");

        var atividade = service.agrupar(List.of(linha));

        assertThat(atividade.resumo()).isEqualTo("Ana cadastrou o usuário.");
        assertThat(atividade.evento().informacaoAlterada()).isEqualTo("Cadastro do usuário");
    }

    @Test
    void incluiColaboradorSemAlteracoesMesmoQuandoHistoricoEstaVazio() {
        when(repository.listarResponsaveis()).thenReturn(List.of());
        when(membros.listarResponsaveisHistorico())
                .thenReturn(List.of(new ResponsavelHistoricoDTO(3, "Monitor Teste")));

        assertThat(service.listarResponsaveis())
                .containsExactly(new ResponsavelHistoricoDTO(3, "Monitor Teste"));
    }

    @Test
    void preservaAutorExcluidoEUsaNomeAtualSemDuplicarColaborador() {
        when(repository.listarResponsaveis()).thenReturn(List.of(
                new ResponsavelHistoricoDTO(1, "Nome antigo"),
                new ResponsavelHistoricoDTO(4, "Bruna removida")));
        when(membros.listarResponsaveisHistorico()).thenReturn(List.of(
                new ResponsavelHistoricoDTO(3, "Monitor Teste"),
                new ResponsavelHistoricoDTO(1, "ana atual")));

        assertThat(service.listarResponsaveis()).containsExactly(
                new ResponsavelHistoricoDTO(1, "ana atual"),
                new ResponsavelHistoricoDTO(4, "Bruna removida"),
                new ResponsavelHistoricoDTO(3, "Monitor Teste"));
    }

    @Test
    void permiteFiltrarPessoaSemAtividadesERetornaPaginaVazia() {
        var filtro = new ConsultaAtividadesDTO(null, null, null, 3, null, null,
                null, null, 10, null, null, PeriodoHistorico.TODOS, null);
        var pagina = PageRequest.of(0, 10);
        when(repository.listar(filtro, pagina)).thenReturn(new PageImpl<>(List.of(), pagina, 0));

        assertThat(service.listar(filtro, pagina)).isEmpty();
        verify(repository).listar(filtro, pagina);
    }

    @Test
    void apresentaCadastroDaChamadaUmaVezSemPerderAlunos() {
        var atividade = service.agrupar(List.of(presenca(1, 10, "CADASTRO"), presenca(2, 11, "CADASTRO")));

        assertThat(atividade.resumo()).isEqualTo("Ana registrou a chamada de 2 alunos.");
        assertThat(atividade.registros()).hasSize(2);
        assertThat(atividade.evento().identificacaoRegistro()).isEqualTo("Manhã (Sede), 21/10/2026");
        assertThat(atividade.evento().identificacaoRegistro()).doesNotContain("ID");
    }

    @Test
    void correcaoIdentificaSomenteAsPresencasQueMudaram() {
        var atividade = service.agrupar(List.of(presenca(3, 10, "EDICAO")));

        assertThat(atividade.resumo()).isEqualTo("Ana corrigiu a chamada de 1 aluno.");
        assertThat(atividade.registros()).extracting(evento -> evento.identificacaoRegistro())
                .allSatisfy(nome -> assertThat(nome).contains("Aluno 10"));
    }

    @Test
    void resumoDaEdicaoDoEventoNaoIncluiValorNumericamenteIgual() {
        var evento = new Linha(90, 8, 1792500000000L, 7, "Ana", "EVENTOS", "evento", "2", 2,
                "EDICAO", "{\"titulo\":\"Festival\",\"valor\":0.0}",
                "{\"titulo\":\"Festival revisado\",\"valor\":0}", "{}", "{}");

        var atividade = service.agrupar(List.of(evento));

        assertThat(atividade.resumo()).isEqualTo("Ana alterou título.");
    }

    @Test
    void leituraDeUmAdministradorNaoUtilizaIdentidadeEnviadaPeloCliente() {
        var filtro = consulta(PeriodoHistorico.TODOS);
        when(repository.consultarLeitura(filtro, 7))
                .thenReturn(new EstadoLeituraHistoricoDTO(20, true, 10));

        service.registrarLeitura(filtro, 7, 15);

        verify(repository).registrarLeitura(filtro.contextoLeitura(), 7, 15);
    }

    @Test
    void identificaNovidadesAPartirDaUltimaLeituraDoAdministrador() {
        var filtro = consulta(PeriodoHistorico.TODOS);
        when(repository.consultarLeitura(filtro, 7))
                .thenReturn(new EstadoLeituraHistoricoDTO(20, true, 10));

        var estado = service.consultarLeitura(filtro, 7);

        assertThat(estado.ultimoLido()).isEqualTo(10);
        assertThat(estado.ultimoEvento()).isEqualTo(20);
        assertThat(estado.temNovidades()).isTrue();
        verify(repository, never()).registrarLeitura(anyString(), anyInt(), anyLong());
    }

    @Test
    void impedeMarcarEventosFuturosComoLidos() {
        var filtro = consulta(PeriodoHistorico.TODOS);
        when(repository.consultarLeitura(filtro, 7))
                .thenReturn(new EstadoLeituraHistoricoDTO(20, true, 10));

        assertThatThrownBy(() -> service.registrarLeitura(filtro, 7, 21))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("eventos disponíveis");
        verify(repository, never()).registrarLeitura(anyString(), anyInt(), anyLong());
    }

    @Test
    void separaLeiturasPorUsuarioEPeriodo() {
        assertThat(consulta(PeriodoHistorico.RECENTES).contextoLeitura())
                .isNotEqualTo(consulta(PeriodoHistorico.ARQUIVADOS).contextoLeitura());
        var outroUsuario = new ConsultaAtividadesDTO(null, null, null, null, null, null,
                null, null, 11, null, null, PeriodoHistorico.TODOS, null);
        assertThat(consulta(PeriodoHistorico.TODOS).contextoLeitura()).isNotEqualTo(outroUsuario.contextoLeitura());
    }

    @Test
    void rejeitaRegistroSemAreaAntesDeConsultarBanco() {
        var filtro = new ConsultaAtividadesDTO(null, null, 10, null, null, null,
                null, null, null, null, null, PeriodoHistorico.TODOS, null);
        assertThatThrownBy(() -> service.listar(filtro, PageRequest.of(0, 10)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Informe a área");
        verifyNoInteractions(repository);
    }

    @Test
    void encaminhaOrdenacaoEPaginaSemDesagruparAtividades() {
        var filtro = consulta(PeriodoHistorico.TODOS);
        for (String campo : List.of("dataHora", "identificacaoRegistro", "nomeResponsavel", "tipoAlteracao")) {
            for (Sort.Direction direcao : Sort.Direction.values()) {
                var pagina = PageRequest.of(2, 5, Sort.by(direcao, campo));
                when(repository.listar(filtro, pagina)).thenReturn(new PageImpl<>(
                        List.of(List.of(presenca(1, 10, "CADASTRO"), presenca(2, 11, "CADASTRO"))), pagina, 30));

                var resultado = service.listar(filtro, pagina);

                assertThat(resultado.getNumber()).isEqualTo(2);
                assertThat(resultado.getTotalElements()).isEqualTo(30);
                assertThat(resultado.getContent()).singleElement().satisfies(atividade ->
                        assertThat(atividade.registros()).hasSize(2));
                verify(repository).listar(filtro, pagina);
            }
        }
    }

    @Test
    void rejeitaOrdenacaoDesconhecidaAntesDeAcessarBanco() {
        var filtro = consulta(PeriodoHistorico.TODOS);
        var pagina = PageRequest.of(0, 10, Sort.by("id; drop table revinfo"));

        assertThatThrownBy(() -> service.listar(filtro, pagina))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Campo de ordenação inválido");
        verifyNoInteractions(repository);
    }

    @Test
    void preservaLimiteDeAtividadesMesmoComOrdenacao() {
        assertThatThrownBy(() -> service.listar(consulta(PeriodoHistorico.TODOS),
                PageRequest.of(0, 51, Sort.by("dataHora"))))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("até 50 atividades");
        verifyNoInteractions(repository);
    }

    @Test
    void gruposCobremTodasAsCategoriasSemMisturarGestaoEConteudoPublico() {
        assertThat(AreaSistemaHistorico.GESTAO_INTERNA.tipos())
                .contains(TipoRegistroHistorico.COLABORADORES, TipoRegistroHistorico.PRESENCAS)
                .doesNotContainAnyElementsOf(AreaSistemaHistorico.CONTEUDO_PUBLICO.tipos());
        assertThat(AreaSistemaHistorico.CONTEUDO_PUBLICO.tipos())
                .contains(TipoRegistroHistorico.EVENTOS, TipoRegistroHistorico.PIX);
        var tipos = new java.util.ArrayList<>(AreaSistemaHistorico.GESTAO_INTERNA.tipos());
        tipos.addAll(AreaSistemaHistorico.CONTEUDO_PUBLICO.tipos());
        assertThat(tipos).containsExactlyInAnyOrder(TipoRegistroHistorico.values());
    }

    @Test
    void consultaDoGrupoPreservaFiltroEPaginacaoESeparaContextoDeLeitura() {
        var todos = consulta(PeriodoHistorico.TODOS);
        for (var area : AreaSistemaHistorico.values()) {
            var filtro = new ConsultaAtividadesDTO(null, null, null, null, null, null,
                    null, null, null, null, null, PeriodoHistorico.TODOS, area);
            var pagina = PageRequest.of(0, 10, Sort.by("nomeResponsavel"));
            when(repository.listar(filtro, pagina)).thenReturn(new PageImpl<>(List.of(), pagina, 0));

            assertThat(service.listar(filtro, pagina)).isEmpty();
            verify(repository).listar(filtro, pagina);
            assertThat(filtro.contextoLeitura()).isNotEqualTo(todos.contextoLeitura());
        }
        assertThat(todos.contextoLeitura()).doesNotContain("GESTAO_INTERNA", "CONTEUDO_PUBLICO");
    }

    @Test
    void rejeitaTipoQueNaoPertenceAoGrupoSelecionado() {
        var filtro = new ConsultaAtividadesDTO(TipoRegistroHistorico.EVENTOS, null, null, null, null, null,
                null, null, null, null, null, PeriodoHistorico.TODOS, AreaSistemaHistorico.GESTAO_INTERNA);

        assertThatThrownBy(() -> service.listar(filtro, PageRequest.of(0, 10)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("não pertence");
        verifyNoInteractions(repository);
    }

    private ConsultaAtividadesDTO consulta(PeriodoHistorico periodo) {
        return new ConsultaAtividadesDTO(null, null, null, null, null, null,
                null, null, 10, null, null, periodo, null);
    }

    private Linha presenca(long id, int usuario, String acao) {
        String dados = "{\"dataRegistro\":\"2026-10-21\",\"presente\":true}";
        String contexto = """
                {"idUsuario":%d,"idTurma":1,"usuario":{"nome_completo":"Aluno %d"},
                 "turma":{"periodo":"MANHA","unidade":"Sede"}}
                """.formatted(usuario, usuario);
        return new Linha(id, 5, 0, 7, "Ana", TipoRegistroHistorico.PRESENCAS.name(),
                "frequencia", String.valueOf(id), null, acao,
                "CADASTRO".equals(acao) ? null : "{\"presente\":false}", dados, contexto, contexto);
    }
}
