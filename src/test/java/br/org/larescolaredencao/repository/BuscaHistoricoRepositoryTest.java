package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.dto.ConsultaAtividadesDTO;
import br.org.larescolaredencao.dto.FiltroHistoricoDTO;
import br.org.larescolaredencao.model.enums.PeriodoHistorico;
import br.org.larescolaredencao.model.enums.TipoRegistroHistorico;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BuscaHistoricoRepositoryTest {

    @Test
    void contatosFicamForaDoGrupoMasPermanecemNoHistoricoDoPerfil() {
        var area = br.org.larescolaredencao.model.enums.AreaSistemaHistorico.USUARIOS_E_ACOMPANHAMENTO;
        assertThat(area.tiposNoContexto(null)).doesNotContain(TipoRegistroHistorico.CONTATOS)
                .contains(TipoRegistroHistorico.VINCULOS_CONTATOS);
        assertThat(area.tiposNoContexto(12)).contains(TipoRegistroHistorico.CONTATOS);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void filtroDeEntrevistasIncluiLegadoEMantemContextoDoUsuario() {
        var jdbcLocal = mock(NamedParameterJdbcTemplate.class);
        when(jdbcLocal.queryForObject(anyString(), anyMap(), eq(Long.class))).thenReturn(0L);
        var filtro = new ConsultaAtividadesDTO(TipoRegistroHistorico.ENTREVISTAS, null, null, null,
                null, null, null, null, 12, null, null, PeriodoHistorico.TODOS,
                br.org.larescolaredencao.model.enums.AreaSistemaHistorico.USUARIOS_E_ACOMPANHAMENTO);
        new AtividadeHistoricoRepository(jdbcLocal, new HistoricoRepository(jdbcLocal))
                .listar(filtro, PageRequest.of(0, 10));
        var sql = ArgumentCaptor.forClass(String.class);
        var parametros = ArgumentCaptor.forClass(Map.class);
        verify(jdbcLocal).queryForObject(sql.capture(), parametros.capture(), eq(Long.class));
        assertThat(sql.getValue()).contains("case when evento.entidade = 'entrevista_social' then 'ENTREVISTAS'",
                "= :categoria", "json_extract(evento.contexto_novo, '$.idUsuario') = :idUsuario");
        assertThat(parametros.getValue()).containsEntry("categoria", "ENTREVISTAS").containsEntry("idUsuario", 12);
        assertThat((java.util.List<String>) parametros.getValue().get("categoriasArea"))
                .containsExactlyInAnyOrder("USUARIOS", "CONTATOS", "VINCULOS_CONTATOS", "PRESENCAS", "OCORRENCIAS", "ENTREVISTAS");
    }

    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);

    @Test
    void buscaAtividadesPeloResponsavelSemPerderBuscaDosDadosOuFiltroDeContexto() {
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Long.class))).thenReturn(0L);
        var filtro = new ConsultaAtividadesDTO(null, " João_% ", null, 7, null, null,
                null, null, 10, null, null, PeriodoHistorico.TODOS, null);

        new AtividadeHistoricoRepository(jdbc, new HistoricoRepository(jdbc))
                .listar(filtro, PageRequest.of(0, 10));

        verificarBusca("busca");
    }

    @Test
    void buscaHistoricoPeloResponsavelPreservandoCategoria() {
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Long.class))).thenReturn(0L);
        var filtro = new FiltroHistoricoDTO(TipoRegistroHistorico.COLABORADORES, " João_% ",
                null, null, null, null, null, null);

        new HistoricoRepository(jdbc).listar(filtro, PageRequest.of(0, 10));

        verificarBusca("search");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void verificarBusca(String parametroDados) {
        var sql = ArgumentCaptor.forClass(String.class);
        var parametros = ArgumentCaptor.forClass(Map.class);
        verify(jdbc).queryForObject(sql.capture(), parametros.capture(), eq(Long.class));
        assertThat(sql.getValue()).contains("evento.pesquisa like :" + parametroDados,
                "or lower(revisao.nome_responsavel) like :nomeResponsavelBusca escape '!'",
                "responsavelBusca.id = revisao.id_membro",
                "lower(responsavelBusca.nome_completo) like :nomeResponsavelBusca escape '!'");
        assertThat(parametros.getValue()).containsEntry(parametroDados, "%joao!_!%%")
                .containsEntry("nomeResponsavelBusca", "%joão!_!%%");
    }
}
