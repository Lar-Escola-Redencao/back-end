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
                   concat(revisao.rev, ':', %s, ':',
                       case when evento.categoria = 'PRESENCAS' then concat(
                           coalesce(json_unquote(json_extract(evento.contexto_novo, '$.idTurma')),
                                    json_unquote(json_extract(evento.contexto_anterior, '$.idTurma')), evento.chave),
                           ':', coalesce(cast(evento.data_aula as char), evento.chave))
                       else coalesce(cast(evento.id_registro as char), concat(evento.entidade, ':', evento.chave))
                       end) as grupo
            from historico_evento evento
            join revinfo revisao on revisao.operacao = evento.operacao
            where revisao.referencia_implantacao = false
            """.formatted(HistoricoRepository.CATEGORIA);

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
                , agrupados as (
                    select grupo, max(revtstmp) as instante, max(id) as ultimo, min(id) as primeiro,
                           max(nome_responsavel) as responsavel,
                           group_concat(distinct case tipo_alteracao
                               when 'CADASTRO' then 'Cadastro'
                               when 'EDICAO' then 'Edição'
                               when 'EXCLUSAO' then 'Exclusão'
                           end order by tipo_alteracao separator ' / ') as acao
                    from selecionados group by grupo
                ), pagina as (
                    select agrupados.*, %s as registro
                    from agrupados join selecionados on selecionados.id = agrupados.primeiro
                    order by %s limit :limite offset :offset
                )
                select selecionados.* from selecionados join pagina using (grupo)
                order by %s, selecionados.id
                """.formatted(identificacaoRegistro(), ordenar(pageable, ""), ordenar(pageable, "pagina."));

        Map<String, List<Linha>> eventos = new java.util.LinkedHashMap<>();
        jdbcTemplate.query(sql, parametros, resultado -> {
            String grupo = resultado.getString("grupo");
            eventos.computeIfAbsent(grupo, chave -> new java.util.ArrayList<>())
                    .add(historicoRepository.mapearEvento(resultado, 0));
        });
        List<List<Linha>> conteudo = List.copyOf(eventos.values());
        long totalElementos = total == null ? 0L : total.longValue();
        return new PageImpl<List<Linha>>(conteudo, pageable, totalElementos);
    }

    private String ordenar(Pageable pageable, String prefixo) {
        if (pageable.getSort().isUnsorted()) {
            return prefixo + "instante desc, " + prefixo + "ultimo desc";
        }
        List<String> campos = new java.util.ArrayList<>();
        for (var ordem : pageable.getSort()) {
            String campo = switch (ordem.getProperty()) {
                case "dataHora" -> "instante";
                case "identificacaoRegistro" -> "registro";
                case "nomeResponsavel" -> "responsavel";
                case "tipoAlteracao" -> "acao";
                default -> throw new IllegalArgumentException("Campo de ordenação inválido.");
            };
            campos.add(prefixo + campo + (ordem.isAscending() ? " asc" : " desc"));
        }
        campos.add(prefixo + "ultimo desc");
        return String.join(", ", campos);
    }

    /** Identificação do primeiro evento do grupo, com os mesmos nomes exibidos no histórico. */
    private String identificacaoRegistro() {
        String dados = "coalesce(selecionados.novo, selecionados.anterior)";
        String contexto = "case when selecionados.novo is null then selecionados.contexto_anterior else selecionados.contexto_novo end";
        String nome = valor(dados, "nomeCompleto", valor(contexto, "nomeCompleto", "'Registro'"));
        String usuario = nomeReferencia(contexto, "usuario", nome);
        String periodo = periodo(dados);
        String turma = periodo("json_extract(" + contexto + ", '$.turma')");
        String grupo = valor(dados, "grupo", "''");
        String titulo = valor(dados, "titulo", valor(dados, "nome",
                valor(contexto, "pagina.titulo", valor(contexto, "pagina.nome", "'Página'"))));
        String data = "coalesce(date_format(" + valor(dados, "dataRegistro", "null") + ", '%d/%m/%Y'), '')";
        return "case selecionados.categoria"
                + " when 'UNIDADES' then " + valor(dados, "nome", "'Unidade'")
                + " when 'TURMAS' then concat(" + periodo + ", ', ', "
                    + valor(contexto, "unidade.titulo", valor(contexto, "unidade.nome", "'Unidade'")) + ")"
                + " when 'EVENTOS' then " + valor(dados, "titulo", valor(contexto, "titulo",
                    valor(contexto, "evento.titulo", valor(contexto, "evento.nome", "'Evento'"))))
                + " when 'DIRETORIA' then " + valor(dados, "nome", "'Registro'")
                + " when 'PARCEIROS' then " + valor(dados, "nome", "'Registro'")
                + " when 'REDES_SOCIAIS' then " + valor(dados, "nome", "'Registro'")
                + " when 'PIX' then 'Doações via Pix'"
                + " when 'GRAFICA' then case when " + grupo + " = 'telefone' then 'Contato da gráfica' else " + titulo + " end"
                + " when 'CONHECA_OSC' then case " + grupo
                    + " when 'historia' then concat('Nossa história, ', " + valor(dados, "titulo", "'null'") + ")"
                    + " when 'carrossel' then 'Imagem do carrossel' when 'texto-sobre' then 'Texto sobre a OSC'"
                    + " else " + valor(dados, "titulo", valor(dados, "nome", "'Conheça a OSC'")) + " end"
                + " when 'TRANSPARENCIA' then " + titulo
                + " when 'USUARIOS' then " + usuario
                + " when 'ENTREVISTAS' then " + usuario
                + " when 'OCORRENCIAS' then " + usuario
                + " when 'PRESENCAS' then concat(" + turma + ", "
                    + "case when " + valor(contexto, "turma.unidade", "''") + " = '' then '' else concat(' (', "
                    + valor(contexto, "turma.unidade", "''") + ", ')') end, ', ', " + data + ")"
                + " when 'VINCULOS_CONTATOS' then concat(" + nomeReferencia(contexto, "contato", "'Contato'")
                    + ", ', contato de ', " + nomeReferencia(contexto, "usuario", "'Usuário'") + ")"
                + " else " + nome + " end";
    }

    private String valor(String fonte, String campo, String padrao) {
        return "coalesce(nullif(json_unquote(json_extract(" + fonte + ", '$." + campo + "')), 'null'), " + padrao + ")";
    }

    private String nomeReferencia(String contexto, String campo, String padrao) {
        return valor(contexto, campo + ".nomeCompleto", valor(contexto, campo + ".nome_completo", padrao));
    }

    private String periodo(String fonte) {
        String periodo = valor(fonte, "periodo", "'Turma'");
        return "case " + periodo + " when 'MANHA' then 'Manhã' when 'TARDE' then 'Tarde' else " + periodo + " end";
    }

    public List<ResponsavelHistoricoDTO> listarResponsaveis() {
        return jdbcTemplate.query("""
                select id_membro, nome_responsavel from revinfo
                where referencia_implantacao = false and id_membro is not null
                  and rev in (select max(rev) from revinfo
                              where referencia_implantacao = false and id_membro is not null group by id_membro)
                order by nome_responsavel
                """, Map.of(), (resultado, numero) -> new ResponsavelHistoricoDTO(
                resultado.getInt("id_membro"), resultado.getString("nome_responsavel")));
    }

    public Page<ResponsavelHistoricoDTO> buscarResponsaveis(String busca, Integer id, Pageable pageable) {
        String base = """
                with ultimas_revisoes as (
                    select id_membro, max(rev) as rev from revinfo revisao
                    where referencia_implantacao = false and id_membro is not null
                      and exists (select 1 from historico_evento evento where evento.operacao = revisao.operacao)
                    group by id_membro
                )
                select revisao.id_membro, coalesce(membro.nome_completo, revisao.nome_responsavel) as nome,
                       revisao.revtstmp, revisao.rev
                from ultimas_revisoes ultima join revinfo revisao on revisao.rev = ultima.rev
                left join membro on membro.id = revisao.id_membro
                where 1 = 1
                """;
        Map<String, Object> parametros = new HashMap<>();
        String termo = TermoBusca.like(busca);
        if (termo != null) {
            base += " and lower(coalesce(membro.nome_completo, revisao.nome_responsavel)) like :busca escape '!'";
            parametros.put("busca", termo);
        }
        if (id != null) {
            base += " and revisao.id_membro = :id";
            parametros.put("id", id);
        }
        Long total = jdbcTemplate.queryForObject("select count(*) from (" + base + ") responsaveis",
                parametros, Long.class);
        parametros.put("limite", pageable.getPageSize());
        parametros.put("offset", pageable.getOffset());
        List<ResponsavelHistoricoDTO> responsaveis = jdbcTemplate.query(base + " order by revisao.revtstmp desc, revisao.rev desc limit :limite offset :offset",
                parametros, (resultado, numero) -> new ResponsavelHistoricoDTO(
                        resultado.getInt("id_membro"), resultado.getString("nome")));
        long totalElementos = total == null ? 0L : total.longValue();
        return new PageImpl<ResponsavelHistoricoDTO>(responsaveis, pageable, totalElementos);
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
        if (filtro.areaSistema() != null) {
            sql.append(" and " + HistoricoRepository.CATEGORIA + " in (:categoriasArea)\n");
            parametros.put("categoriasArea", filtro.areaSistema().tiposNoContexto(filtro.idUsuario()).stream().map(Enum::name).toList());
        }
        if (filtro.tipoRegistro() != null) {
            sql.append(" and " + HistoricoRepository.CATEGORIA + " = :categoria\n");
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
            sql.append("""
                     or lower(revisao.nome_responsavel) like :nomeResponsavelBusca escape '!'
                     or exists (select 1 from membro responsavelBusca
                                where responsavelBusca.id = revisao.id_membro
                                  and lower(responsavelBusca.nome_completo) like :nomeResponsavelBusca escape '!')
                    """);
            parametros.put("nomeResponsavelBusca", TermoBusca.like(filtro.search()));
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
