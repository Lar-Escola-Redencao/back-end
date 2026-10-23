package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.audit.HistoricoHibernateListener;
import br.org.larescolaredencao.dto.FiltroHistoricoDTO;
import br.org.larescolaredencao.service.TermoBusca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
public class HistoricoRepository {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private static final String TABELAS = """
            from historico_evento evento
            join revinfo revisao on revisao.operacao = evento.operacao
            """;

    private static final String CONSULTA_EVENTOS = """
            select evento.*, revisao.rev, revisao.revtstmp, revisao.id_membro, revisao.nome_responsavel
            """ + TABELAS;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public record Linha(
            long id,
            int revisao,
            long instante,
            Integer idResponsavel,
            String nomeResponsavel,
            String categoria,
            String entidade,
            String chave,
            Integer idRegistro,
            String tipoAlteracao,
            String anterior,
            String novo,
            String contextoAnterior,
            String contextoNovo) {
    }

    public HistoricoRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Page<Linha> listar(FiltroHistoricoDTO filtro, Pageable pageable) {
        StringBuilder condicoes = new StringBuilder("""
                where revisao.referencia_implantacao = false
                  and evento.categoria = :categoria
                """);
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("categoria", filtro.tipoRegistro().name());

        adicionarFiltros(filtro, condicoes, parametros);
        adicionarBusca(filtro.search(), condicoes, parametros);

        String consultaTotal = "select count(*) " + TABELAS + condicoes;
        long total = Objects.requireNonNull(
                jdbcTemplate.queryForObject(consultaTotal, parametros, Long.class));

        parametros.put("limite", pageable.getPageSize());
        parametros.put("offset", pageable.getOffset());

        String consulta = CONSULTA_EVENTOS + condicoes + """
                order by revisao.revtstmp desc, revisao.rev desc, evento.id desc
                limit :limite offset :offset
                """;
        List<Linha> eventos = jdbcTemplate.query(consulta, parametros, this::mapearEvento);
        return new PageImpl<>(eventos, pageable, total);
    }

    public Optional<Linha> buscar(long id) {
        String consulta = CONSULTA_EVENTOS + """
                where evento.id = :id
                  and revisao.referencia_implantacao = false
                """;

        return jdbcTemplate.query(consulta, Map.of("id", id), this::mapearEvento)
                .stream()
                .findFirst();
    }

    private void adicionarFiltros(
            FiltroHistoricoDTO filtro,
            StringBuilder condicoes,
            Map<String, Object> parametros) {

        if (filtro.idRegistro() != null) {
            condicoes.append(" and evento.id_registro = :registro\n");
            parametros.put("registro", filtro.idRegistro());
        }

        if (filtro.idResponsavel() != null) {
            condicoes.append(" and revisao.id_membro = :responsavel\n");
            parametros.put("responsavel", filtro.idResponsavel());
        }

        if (filtro.dataInicial() != null) {
            long inicio = filtro.dataInicial().atStartOfDay(FUSO).toInstant().toEpochMilli();
            condicoes.append(" and revisao.revtstmp >= :inicio\n");
            parametros.put("inicio", inicio);
        }

        if (filtro.dataFinal() != null) {
            long fimExclusivo = filtro.dataFinal().plusDays(1).atStartOfDay(FUSO).toInstant().toEpochMilli();
            condicoes.append(" and revisao.revtstmp < :fim\n");
            parametros.put("fim", fimExclusivo);
        }

        if (filtro.tipoAlteracao() != null) {
            condicoes.append(" and evento.tipo_alteracao = :alteracao\n");
            parametros.put("alteracao", filtro.tipoAlteracao().name());
        }

        if (filtro.dataAula() != null) {
            condicoes.append(" and evento.data_aula = :aula\n");
            parametros.put("aula", filtro.dataAula());
        }
    }

    private void adicionarBusca(
            String termo,
            StringBuilder condicoes,
            Map<String, Object> parametros) {

        if (termo == null || termo.isBlank()) {
            return;
        }

        condicoes.append(" and (evento.pesquisa like :search escape '!'");
        parametros.put("search", TermoBusca.like(HistoricoHibernateListener.normalizarPesquisa(termo)));

        String digitos = TermoBusca.digitosLike(termo);
        if (digitos != null) {
            condicoes.append(" or evento.pesquisa like :digitos escape '!'");
            parametros.put("digitos", digitos);
        }

        condicoes.append(")\n");
    }

    Linha mapearEvento(ResultSet resultado, int numeroLinha) throws SQLException {
        return new Linha(
                resultado.getLong("id"),
                resultado.getInt("rev"),
                resultado.getLong("revtstmp"),
                resultado.getObject("id_membro", Integer.class),
                resultado.getString("nome_responsavel"),
                resultado.getString("categoria"),
                resultado.getString("entidade"),
                resultado.getString("chave"),
                resultado.getObject("id_registro", Integer.class),
                resultado.getString("tipo_alteracao"),
                resultado.getString("anterior"),
                resultado.getString("novo"),
                resultado.getString("contexto_anterior"),
                resultado.getString("contexto_novo"));
    }
}
