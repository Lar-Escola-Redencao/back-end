package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.audit.HistoricoHibernateListener;
import br.org.larescolaredencao.dto.ConsultaAtividadesDTO;
import br.org.larescolaredencao.dto.EstadoLeituraHistoricoDTO;
import br.org.larescolaredencao.dto.ResponsavelHistoricoDTO;
import br.org.larescolaredencao.model.enums.PeriodoHistorico;
import br.org.larescolaredencao.repository.HistoricoRepository.Linha;
import br.org.larescolaredencao.service.TermoBusca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AtividadeHistoricoRepository {

    private static final String BASE = """
            select evento.*, revisao.rev, revisao.revtstmp, revisao.id_membro, revisao.nome_responsavel,
                   concat(revisao.rev, ':', evento.categoria, ':',
                       case when evento.categoria = 'PRESENCAS' then concat(
                           coalesce(json_unquote(json_extract(evento.contexto_novo, '$.idTurma')),
                                    json_unquote(json_extract(evento.contexto_anterior, '$.idTurma')), evento.chave),
                           ':', coalesce(cast(evento.data_aula as char), evento.chave))
                       else coalesce(cast(evento.id_registro as char), concat(evento.entidade, ':', evento.chave))
                       end) as grupo
            from historico_evento evento
            join revinfo revisao on revisao.operacao = evento.operacao
            where revisao.referencia_implantacao = false
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final HistoricoRepository historicoRepository;

    private record Consulta(String sql, Map<String, Object> parametros) {
    }

    public AtividadeHistoricoRepository(NamedParameterJdbcTemplate jdbcTemplate, HistoricoRepository historicoRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.historicoRepository = historicoRepository;
    }

    public Page<List<Linha>> listar(ConsultaAtividadesDTO filtro, Pageable pageable) {
        Consulta consulta = consultar(filtro);
        String grupos = "with selecionados as (" + consulta.sql() + ") ";
        Long total = jdbcTemplate.queryForObject(grupos + "select count(distinct grupo) from selecionados",
                consulta.parametros(), Long.class);

        Map<String, Object> parametros = new HashMap<>(consulta.parametros());
        parametros.put("limite", pageable.getPageSize());
        parametros.put("offset", pageable.getOffset());
        String sql = grupos + """
                , pagina as (
                    select grupo, max(revtstmp) as instante, max(id) as ultimo
                    from selecionados group by grupo
                    order by instante desc, ultimo desc limit :limite offset :offset
                )
                select selecionados.* from selecionados join pagina using (grupo)
                order by pagina.instante desc, pagina.ultimo desc, selecionados.id
                """;

        Map<String, List<Linha>> eventos = new java.util.LinkedHashMap<>();
        jdbcTemplate.query(sql, parametros, resultado -> {
            String grupo = resultado.getString("grupo");
            eventos.computeIfAbsent(grupo, chave -> new java.util.ArrayList<>())
                    .add(historicoRepository.mapearEvento(resultado, 0));
        });
        return new PageImpl<>(List.copyOf(eventos.values()), pageable, total == null ? 0 : total);
    }

    public List<ResponsavelHistoricoDTO> listarResponsaveis() {
        return jdbcTemplate.query("""
                select id_membro, nome_responsavel from revinfo
                where referencia_implantacao = false and id_membro is not null
                  and rev in (select max(rev) from revinfo where id_membro is not null group by id_membro)
                order by nome_responsavel
                """, Map.of(), (resultado, numero) -> new ResponsavelHistoricoDTO(
                resultado.getInt("id_membro"), resultado.getString("nome_responsavel")));
    }

    public EstadoLeituraHistoricoDTO consultarLeitura(ConsultaAtividadesDTO filtro, int administrador) {
        Consulta consulta = consultar(filtro);
        Map<String, Object> parametros = new HashMap<>(consulta.parametros());
        parametros.put("administrador", administrador);
        parametros.put("contexto", filtro.contextoLeitura());
        return jdbcTemplate.queryForObject("""
                select coalesce(max(eventos.id), 0) as ultimo,
                       coalesce(max(eventos.id), 0) > coalesce((
                           select ultimo_evento from historico_leitura
                           where id_membro = :administrador and contexto = :contexto
                       ), 0) as novidades,
                       coalesce((select ultimo_evento from historico_leitura
                           where id_membro = :administrador and contexto = :contexto), 0) as ultimo_lido
                from (
                """ + consulta.sql() + ") eventos", parametros,
                (resultado, numero) -> new EstadoLeituraHistoricoDTO(
                        resultado.getLong("ultimo"), resultado.getBoolean("novidades"), resultado.getLong("ultimo_lido")));
    }

    public void registrarLeitura(String contexto, int administrador, long ultimoEvento) {
        jdbcTemplate.update("""
                insert into historico_leitura (id_membro, contexto, ultimo_evento)
                values (:administrador, :contexto, :ultimo)
                on duplicate key update ultimo_evento = greatest(ultimo_evento, :ultimo)
                """, Map.of("administrador", administrador, "contexto", contexto, "ultimo", ultimoEvento));
    }

    private Consulta consultar(ConsultaAtividadesDTO filtro) {
        StringBuilder sql = new StringBuilder(BASE);
        Map<String, Object> parametros = new HashMap<>();
        if (filtro.tipoRegistro() != null) {
            sql.append(" and evento.categoria = :categoria\n");
            parametros.put("categoria", filtro.tipoRegistro().name());
        }
        if (filtro.idRegistro() != null) {
            sql.append(" and evento.id_registro = :registro\n");
            parametros.put("registro", filtro.idRegistro());
        }
        if (filtro.idResponsavel() != null) {
            sql.append(" and revisao.id_membro = :responsavel\n");
            parametros.put("responsavel", filtro.idResponsavel());
        }
        adicionarContexto(sql, parametros, "idUsuario", filtro.idUsuario());
        adicionarContexto(sql, parametros, "idTurma", filtro.idTurma());
        if (filtro.idContato() != null) {
            sql.append("""
                     and ((evento.categoria = 'CONTATOS' and evento.id_registro = :idContato)
                         or json_extract(evento.contexto_novo, '$.idContato') = :idContato
                         or json_extract(evento.contexto_anterior, '$.idContato') = :idContato)
                    """);
            parametros.put("idContato", filtro.idContato());
        }
        if (filtro.dataAula() != null) {
            sql.append(" and evento.data_aula = :aula\n");
            parametros.put("aula", filtro.dataAula());
        }
        if (filtro.tipoAlteracao() != null) {
            sql.append(" and evento.tipo_alteracao = :acao\n");
            parametros.put("acao", filtro.tipoAlteracao().name());
        }
        if (filtro.dataInicial() != null) {
            sql.append(" and revisao.revtstmp >= :inicio\n");
            parametros.put("inicio", inicioDoDia(filtro.dataInicial()));
        }
        if (filtro.dataFinal() != null) {
            sql.append(" and revisao.revtstmp < :fim\n");
            parametros.put("fim", inicioDoDia(filtro.dataFinal().plusDays(1)));
        }
        if (filtro.periodoEfetivo() != PeriodoHistorico.TODOS) {
            sql.append(filtro.periodoEfetivo() == PeriodoHistorico.RECENTES
                    ? " and revisao.revtstmp >= :corte\n" : " and revisao.revtstmp < :corte\n");
            parametros.put("corte", inicioDoDia(LocalDate.now(HistoricoRepository.FUSO).minusMonths(6)));
        }
        if (filtro.search() != null && !filtro.search().isBlank()) {
            sql.append(" and (evento.pesquisa like :busca escape '!'");
            parametros.put("busca", TermoBusca.like(HistoricoHibernateListener.normalizarPesquisa(filtro.search())));
            String digitos = TermoBusca.digitosLike(filtro.search());
            if (digitos != null) {
                sql.append(" or evento.pesquisa like :digitos escape '!'");
                parametros.put("digitos", digitos);
            }
            sql.append(")\n");
        }
        return new Consulta(sql.toString(), parametros);
    }

    private void adicionarContexto(StringBuilder sql, Map<String, Object> parametros, String campo, Integer valor) {
        if (valor == null) {
            return;
        }
        sql.append(" and (json_extract(evento.contexto_novo, '$.").append(campo).append("') = :").append(campo)
                .append(" or json_extract(evento.contexto_anterior, '$.").append(campo).append("') = :")
                .append(campo);
        if ("idUsuario".equals(campo)) {
            sql.append("""
                     or json_contains(evento.contexto_novo, json_object('id', :idUsuario), '$.usuariosVinculados')
                     or json_contains(evento.contexto_anterior, json_object('id', :idUsuario), '$.usuariosVinculados')
                    """);
        }
        sql.append(")\n");
        parametros.put(campo, valor);
    }

    private long inicioDoDia(LocalDate data) {
        return data.atStartOfDay(HistoricoRepository.FUSO).toInstant().toEpochMilli();
    }
}
