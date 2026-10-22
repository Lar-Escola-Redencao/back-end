package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.ConsultaAtividadesDTO;
import br.org.larescolaredencao.dto.EstadoLeituraHistoricoDTO;
import br.org.larescolaredencao.model.enums.PeriodoHistorico;
import br.org.larescolaredencao.model.enums.TipoRegistroHistorico;
import br.org.larescolaredencao.repository.AtividadeHistoricoRepository;
import br.org.larescolaredencao.repository.HistoricoRepository;
import br.org.larescolaredencao.repository.HistoricoRepository.Linha;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AtividadeHistoricoServiceTest {

    private final AtividadeHistoricoRepository repository = mock(AtividadeHistoricoRepository.class);
    private final HistoricoService historico = new HistoricoService(mock(HistoricoRepository.class));
    private final AtividadeHistoricoService service = new AtividadeHistoricoService(repository, historico);

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
                null, null, 11, null, null, PeriodoHistorico.TODOS);
        assertThat(consulta(PeriodoHistorico.TODOS).contextoLeitura()).isNotEqualTo(outroUsuario.contextoLeitura());
    }

    @Test
    void rejeitaRegistroSemAreaAntesDeConsultarBanco() {
        var filtro = new ConsultaAtividadesDTO(null, null, 10, null, null, null,
                null, null, null, null, null, PeriodoHistorico.TODOS);
        assertThatThrownBy(() -> service.listar(filtro, PageRequest.of(0, 10)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Informe a área");
        verifyNoInteractions(repository);
    }

    private ConsultaAtividadesDTO consulta(PeriodoHistorico periodo) {
        return new ConsultaAtividadesDTO(null, null, null, null, null, null,
                null, null, 10, null, null, periodo);
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
