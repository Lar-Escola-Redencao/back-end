package br.org.larescolaredencao.audit;

import br.org.larescolaredencao.model.ContatoUsuarioId;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Evento;
import br.org.larescolaredencao.model.EventoRedeSocialId;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PostCollectionRemoveEvent;
import org.hibernate.event.spi.PostCollectionRemoveEventListener;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.event.spi.PreCollectionRemoveEvent;
import org.hibernate.event.spi.PreCollectionRemoveEventListener;
import org.hibernate.event.spi.PreCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class HistoricoHibernateListener implements PostInsertEventListener, PostUpdateEventListener,
        PostDeleteEventListener, PreCollectionUpdateEventListener, PreCollectionRemoveEventListener,
        PostCollectionUpdateEventListener, PostCollectionRemoveEventListener, PostCollectionRecreateEventListener {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    @Override
    public void onPostInsert(PostInsertEvent evento) {
        EntidadeAuditada.porClasse(evento.getPersister().getMappedClass()).ifPresent(entidade -> {
            Map<String, Object> valoresNovos = extrairValores(
                    evento.getPersister(), evento.getState(), evento.getId(), evento.getSession());
            registrarAlteracao(evento.getSession(), entidade, evento.getId(), null, valoresNovos);
        });
    }

    @Override
    public void onPostUpdate(PostUpdateEvent evento) {
        EntidadeAuditada.porClasse(evento.getPersister().getMappedClass()).ifPresent(entidade -> {
            Map<String, Object> valoresAnteriores = extrairValores(
                    evento.getPersister(), evento.getOldState(), evento.getId(), evento.getSession());
            Map<String, Object> valoresNovos = extrairValores(
                    evento.getPersister(), evento.getState(), evento.getId(), evento.getSession());

            registrarAlteracao(evento.getSession(), entidade, evento.getId(), valoresAnteriores, valoresNovos);
        });
    }

    @Override
    public void onPostDelete(PostDeleteEvent evento) {
        EntidadeAuditada.porClasse(evento.getPersister().getMappedClass()).ifPresent(entidade -> {
            Map<String, Object> valoresAnteriores = extrairValores(
                    evento.getPersister(), evento.getDeletedState(), evento.getId(), evento.getSession());
            registrarAlteracao(evento.getSession(), entidade, evento.getId(), valoresAnteriores, null);
        });
    }

    private Map<String, Object> extrairValores(
            EntityPersister persister,
            Object[] estado,
            Object id,
            SharedSessionContractImplementor sessao) {

        if (estado == null) {
            return null;
        }

        Map<String, Object> valores = new LinkedHashMap<>();
        if (id instanceof ContatoUsuarioId chaveComposta) {
            valores.put("idUsuario", chaveComposta.getIdUsuario());
            valores.put("idContato", chaveComposta.getIdContato());
        } else if (id instanceof EventoRedeSocialId chaveEvento) {
            valores.put("idEvento", chaveEvento.getIdEvento());
            valores.put("idRedeSocial", chaveEvento.getIdRedeSocial());
        } else {
            valores.put("id", id);
        }

        String[] nomes = persister.getPropertyNames();
        var tipos = persister.getPropertyTypes();

        for (int indice = 0; indice < nomes.length; indice++) {
            if (nomes[indice].equals("senha") || tipos[indice].isCollectionType()) {
                continue;
            }

            Object valor = estado[indice];
            if (valor != null && tipos[indice].isEntityType()) {
                valor = obterIdRelacionamento(valor, sessao);
            }
            if (valor instanceof String texto && List.of("telefone", "cpf").contains(nomes[indice])) {
                valor = texto.replaceAll("\\D", "");
            }
            valores.put(nomes[indice], converterValor(valor));
        }

        return valores;
    }

    private Object obterIdRelacionamento(Object entidade, SharedSessionContractImplementor sessao) {
        if (entidade instanceof HibernateProxy proxy) {
            return proxy.getHibernateLazyInitializer().getIdentifier();
        }

        EntityPersister persister = sessao.getEntityPersister(null, entidade);
        return persister.getIdentifier(entidade, sessao);
    }

    private Object converterValor(Object valor) {
        if (valor == null || valor instanceof Number || valor instanceof Boolean) {
            return valor;
        }
        return valor.toString();
    }

    private void registrarAlteracao(
            SharedSessionContractImplementor sessao,
            EntidadeAuditada entidade,
            Object id,
            Map<String, Object> valoresAnteriores,
            Map<String, Object> valoresNovos) {

        if (ComparadorAuditoria.equivalentes(valoresAnteriores, valoresNovos)) {
            return;
        }

        sessao.doWork(conexao -> {
            Map<String, Object> contextoAnterior = montarContexto(conexao, entidade, valoresAnteriores);
            Map<String, Object> contextoNovo = montarContexto(conexao, entidade, valoresNovos);
            String chave = montarChave(id);

            gravarEvento(conexao, entidade, entidade.tabela, chave,
                    valoresAnteriores, valoresNovos, contextoAnterior, contextoNovo);
        });
    }

    private String montarChave(Object id) {
        if (id instanceof ContatoUsuarioId chaveComposta) {
            return chaveComposta.getIdUsuario() + ":" + chaveComposta.getIdContato();
        }
        if (id instanceof EventoRedeSocialId chaveEvento) {
            return chaveEvento.getIdEvento() + ":" + chaveEvento.getIdRedeSocial();
        }
        return id.toString();
    }

    private Map<String, Object> montarContexto(
            Connection conexao,
            EntidadeAuditada entidade,
            Map<String, Object> valores) throws SQLException {

        Map<String, Object> contexto = new LinkedHashMap<>();
        if (valores == null) {
            return contexto;
        }

        Object idUsuario = obterIdUsuario(entidade, valores);
        Object idTurma = valores.get("turma");

        if (entidade == EntidadeAuditada.MEMBRO && valores.get("papel") != null) {
            contexto.put("papel", consultarRegistro(conexao,
                    "select nome_papel as nome from papel where id = ?", valores.get("papel")));
        }

        if (valores.get("matricula") != null) {
            Map<String, Object> matricula = consultarRegistro(conexao,
                    "select id_usuario, id_turma from matricula where id = ?", valores.get("matricula"));
            idUsuario = matricula.get("id_usuario");
            idTurma = matricula.get("id_turma");
        }

        if (idUsuario != null) {
            Map<String, Object> usuario = entidade == EntidadeAuditada.USUARIO
                    ? valores
                    : consultarRegistro(conexao, """
                            select nome_completo, cpf, documento_auxiliar, endereco, bairro, escola
                            from usuario where id = ?
                            """, idUsuario);

            contexto.put("idUsuario", idUsuario);
            contexto.put("usuario", usuario);
        }

        if (idTurma != null) {
            contexto.put("idTurma", idTurma);
            contexto.put("turma", consultarRegistro(conexao, """
                    select turma.periodo, turma.id_unidade, unidade.nome as unidade
                    from turma
                    join unidade on unidade.id = turma.id_unidade
                    where turma.id = ?
                    """, idTurma));
        }

        if (entidade == EntidadeAuditada.TURMA && valores.get("unidade") != null) {
            contexto.put("unidade", consultarRegistro(conexao,
                    "select nome from unidade where id = ?", valores.get("unidade")));
        }

        adicionarContextoContato(conexao, entidade, valores, contexto);
        contexto.putAll(ConteudoPublicoAuditoria.montarContexto(conexao, entidade, valores));

        if (valores.get("membro") != null) {
            contexto.put("autorOriginal", consultarRegistro(conexao,
                    "select id, nome_completo from membro where id = ?", valores.get("membro")));
        }

        return contexto;
    }

    private Object obterIdUsuario(EntidadeAuditada entidade, Map<String, Object> valores) {
        if (entidade == EntidadeAuditada.FICHA || entidade == EntidadeAuditada.USUARIO) {
            return valores.get("id");
        }
        if (valores.get("usuario") != null) {
            return valores.get("usuario");
        }
        return valores.get("idUsuario");
    }

    private void adicionarContextoContato(
            Connection conexao,
            EntidadeAuditada entidade,
            Map<String, Object> valores,
            Map<String, Object> contexto) throws SQLException {

        Object idContato = valores.containsKey("idContato") ? valores.get("idContato") : valores.get("contato");
        if (idContato != null) {
            contexto.put("idContato", idContato);
            contexto.put("contato", consultarRegistro(conexao, """
                    select nome_completo, cpf, telefone, email, endereco, local_trabalho
                    from contato where id = ?
                    """, idContato));
        }

        if (entidade == EntidadeAuditada.CONTATO) {
            contexto.put("usuariosVinculados", consultarUsuariosVinculados(conexao, valores.get("id")));
        }
    }

    private List<Map<String, Object>> consultarUsuariosVinculados(Connection conexao, Object idContato)
            throws SQLException {

        String sql = """
                select usuario.id, usuario.nome_completo, usuario.cpf
                from contato_usuario vinculo
                join usuario on usuario.id = vinculo.id_usuario
                where vinculo.id_contato = ?
                order by usuario.id
                """;
        List<Map<String, Object>> usuarios = new ArrayList<>();

        try (var consulta = conexao.prepareStatement(sql)) {
            consulta.setObject(1, idContato);

            try (var resultado = consulta.executeQuery()) {
                while (resultado.next()) {
                    Map<String, Object> usuario = new LinkedHashMap<>();
                    usuario.put("id", resultado.getInt("id"));
                    usuario.put("nome", resultado.getString("nome_completo"));
                    usuario.put("cpf", resultado.getString("cpf"));
                    usuarios.add(usuario);
                }
            }
        }

        return usuarios;
    }

    private Map<String, Object> consultarRegistro(Connection conexao, String sql, Object id) throws SQLException {
        Map<String, Object> registro = new LinkedHashMap<>();

        try (var consulta = conexao.prepareStatement(sql)) {
            consulta.setObject(1, id);

            try (var resultado = consulta.executeQuery()) {
                if (resultado.next()) {
                    var metadados = resultado.getMetaData();
                    for (int indice = 1; indice <= metadados.getColumnCount(); indice++) {
                        registro.put(metadados.getColumnLabel(indice), resultado.getObject(indice));
                    }
                }
            }
        }

        return registro;
    }

    private void gravarEvento(
            Connection conexao,
            EntidadeAuditada entidade,
            String tabela,
            String chave,
            Map<String, Object> valoresAnteriores,
            Map<String, Object> valoresNovos,
            Map<String, Object> contextoAnterior,
            Map<String, Object> contextoNovo) throws SQLException {

        OperacaoAuditoria operacao = OperacaoAuditoria.atual();
        String chaveAlteracao = tabela + ":" + chave;
        operacao.alteracoes.putIfAbsent(chaveAlteracao,
                new OperacaoAuditoria.Alteracao(valoresAnteriores, contextoAnterior));

        OperacaoAuditoria.Alteracao primeiraAlteracao = operacao.alteracoes.get(chaveAlteracao);
        valoresAnteriores = primeiraAlteracao.anterior();
        contextoAnterior = primeiraAlteracao.contextoAnterior();

        if (ComparadorAuditoria.equivalentes(valoresAnteriores, valoresNovos)) {
            removerEventoSemAlteracao(conexao, operacao.id, tabela, chave);
            return;
        }

        Map<String, Object> valoresReferencia = valoresNovos != null ? valoresNovos : valoresAnteriores;
        Map<String, Object> contextoReferencia = valoresNovos != null ? contextoNovo : contextoAnterior;
        Object idRegistro = obterIdRegistro(entidade, valoresReferencia, contextoReferencia);
        String tipoAlteracao = obterTipoAlteracao(valoresAnteriores, valoresNovos);
        String pesquisa = textoPesquisavel(Arrays.asList(
                valoresAnteriores, valoresNovos, contextoAnterior, contextoNovo));
        Object dataAula = entidade == EntidadeAuditada.FREQUENCIA ? valoresReferencia.get("dataRegistro") : null;

        String sql = """
                insert into historico_evento(
                    operacao, categoria, entidade, chave, id_registro, tipo_alteracao,
                    anterior, novo, contexto_anterior, contexto_novo, pesquisa, data_aula)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on duplicate key update
                    id_registro = values(id_registro),
                    tipo_alteracao = values(tipo_alteracao),
                    novo = values(novo),
                    contexto_novo = values(contexto_novo),
                    pesquisa = values(pesquisa),
                    data_aula = values(data_aula)
                """;

        try (var comando = conexao.prepareStatement(sql)) {
            comando.setString(1, operacao.id);
            comando.setString(2, ConteudoPublicoAuditoria.categoria(entidade, contextoReferencia).name());
            comando.setString(3, tabela);
            comando.setString(4, chave);
            comando.setObject(5, idRegistro);
            comando.setString(6, tipoAlteracao);
            comando.setString(7, valoresAnteriores == null ? null : jsonMapper.writeValueAsString(valoresAnteriores));
            comando.setString(8, valoresNovos == null ? null : jsonMapper.writeValueAsString(valoresNovos));
            comando.setString(9, jsonMapper.writeValueAsString(contextoAnterior));
            comando.setString(10, jsonMapper.writeValueAsString(contextoNovo));
            comando.setString(11, pesquisa);
            comando.setObject(12, dataAula);
            comando.executeUpdate();
        }
    }

    private Object obterIdRegistro(
            EntidadeAuditada entidade,
            Map<String, Object> valores,
            Map<String, Object> contexto) {

        return switch (entidade) {
            case LINK_EVENTO -> valores.get("idEvento");
            case PAGINA, SECAO, DOCUMENTO -> contexto.get("idPagina");
            case MATRICULA, FICHA, COMPOSICAO, ARQUIVO -> contexto.get("idUsuario");
            case VINCULO -> valores.get("idContato");
            case FREQUENCIA -> null;
            default -> valores.get("id");
        };
    }

    private String obterTipoAlteracao(Map<String, Object> anteriores, Map<String, Object> novos) {
        if (anteriores == null) {
            return "CADASTRO";
        }
        if (novos == null) {
            return "EXCLUSAO";
        }
        return "EDICAO";
    }

    private void removerEventoSemAlteracao(Connection conexao, String operacao, String tabela, String chave)
            throws SQLException {

        String sql = "delete from historico_evento where operacao = ? and entidade = ? and chave = ?";
        try (var comando = conexao.prepareStatement(sql)) {
            comando.setString(1, operacao);
            comando.setString(2, tabela);
            comando.setString(3, chave);
            comando.executeUpdate();
        }
    }

    public static String normalizarPesquisa(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    public static String textoPesquisavel(Object valor) {
        if (valor == null) {
            return "";
        }
        if (valor instanceof Map<?, ?> valores) {
            return textoPesquisavel(valores.values());
        }
        if (valor instanceof Collection<?> valores) {
            return valores.stream()
                    .map(HistoricoHibernateListener::textoPesquisavel)
                    .collect(Collectors.joining(" | "));
        }

        String texto = normalizarPesquisa(valor.toString());
        if (texto.matches("\\d{4}-\\d{2}-\\d{2}")) {
            String dataFormatada = LocalDate.parse(texto).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            return texto + " | " + dataFormatada;
        }
        if (texto.matches("[0-9\\s.\\-()/]+")) {
            return texto + " | " + texto.replaceAll("\\D", "");
        }
        return texto;
    }

    private boolean possuiVinculosDeUnidades(AbstractCollectionEvent evento) {
        String relacionamento = evento.getCollection().getRole();
        return evento.getAffectedOwnerOrNull() instanceof Membro
                && (relacionamento == null || relacionamento.endsWith("Membro.unidades"));
    }

    private Map<String, Object> consultarUnidadesAtuais(Connection conexao, Object idMembro) throws SQLException {
        String sql = """
                select unidade.id, unidade.nome
                from membro_unidade vinculo
                join unidade on unidade.id = vinculo.id_unidade
                where vinculo.id_membro = ?
                order by unidade.id
                """;
        List<Map<String, Object>> unidades = new ArrayList<>();

        try (var consulta = conexao.prepareStatement(sql)) {
            consulta.setObject(1, idMembro);

            try (var resultado = consulta.executeQuery()) {
                while (resultado.next()) {
                    unidades.add(Map.of("id", resultado.getInt("id"), "nome", resultado.getString("nome")));
                }
            }
        }

        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put("id", idMembro);
        valores.put("unidades", unidades);
        return valores;
    }

    private void guardarUnidadesAnteriores(AbstractCollectionEvent evento) {
        if (!possuiVinculosDeUnidades(evento)) {
            return;
        }

        Integer idMembro = (Integer) evento.getAffectedOwnerIdOrNull();
        evento.getSession().doWork(conexao -> {
            Map<String, Object> unidades = consultarUnidadesAtuais(conexao, idMembro);
            OperacaoAuditoria.atual().unidadesAnteriores.putIfAbsent(idMembro, unidades);
        });
    }

    private void registrarMudancaDeUnidades(AbstractCollectionEvent evento) {
        if (!possuiVinculosDeUnidades(evento)) {
            return;
        }

        Integer idMembro = (Integer) evento.getAffectedOwnerIdOrNull();
        evento.getSession().doWork(conexao -> {
            Map<String, Object> unidadesAnteriores = OperacaoAuditoria.atual().unidadesAnteriores.get(idMembro);
            if (unidadesAnteriores == null) {
                unidadesAnteriores = Map.of("id", idMembro, "unidades", List.of());
            }

            Map<String, Object> unidadesNovas = consultarUnidadesAtuais(conexao, idMembro);
            Membro membro = (Membro) evento.getAffectedOwnerOrNull();
            Map<String, Object> contexto = new LinkedHashMap<>();
            contexto.put("nomeCompleto", membro.getNomeCompleto());
            contexto.put("id", idMembro);

            gravarEvento(conexao, EntidadeAuditada.MEMBRO, "membro_unidade", idMembro.toString(),
                    unidadesAnteriores, unidadesNovas, contexto, contexto);
        });
    }

    private boolean possuiParceirosDoEvento(AbstractCollectionEvent evento) {
        String relacionamento = evento.getCollection().getRole();
        return evento.getAffectedOwnerOrNull() instanceof Evento
                && (relacionamento == null || relacionamento.endsWith("Evento.parceiros"));
    }

    private Map<String, Object> consultarParceirosAtuais(Connection conexao, Integer idEvento) throws SQLException {
        List<Map<String, Object>> parceiros = new ArrayList<>();
        try (var consulta = conexao.prepareStatement("""
                select parceiro.id, parceiro.nome from parceiro_evento vinculo
                join parceiro on parceiro.id = vinculo.id_parceiro
                where vinculo.id_evento = ? order by parceiro.id
                """)) {
            consulta.setInt(1, idEvento);
            try (var resultado = consulta.executeQuery()) {
                while (resultado.next()) {
                    parceiros.add(Map.of("id", resultado.getInt("id"), "nome", resultado.getString("nome")));
                }
            }
        }
        return Map.of("id", idEvento, "parceiros", parceiros);
    }

    private void guardarParceirosAnteriores(AbstractCollectionEvent evento) {
        if (!possuiParceirosDoEvento(evento)) return;
        Integer idEvento = (Integer) evento.getAffectedOwnerIdOrNull();
        evento.getSession().doWork(conexao -> OperacaoAuditoria.atual().parceirosAnteriores
                .putIfAbsent(idEvento, consultarParceirosAtuais(conexao, idEvento)));
    }

    private void registrarMudancaDeParceiros(AbstractCollectionEvent evento) {
        if (!possuiParceirosDoEvento(evento)) return;
        Integer idEvento = (Integer) evento.getAffectedOwnerIdOrNull();
        evento.getSession().doWork(conexao -> {
            OperacaoAuditoria operacao = OperacaoAuditoria.atual();
            Map<String, Object> anterior = operacao.parceirosAnteriores.get(idEvento);
            var cadastroEvento = operacao.alteracoes.get("evento:" + idEvento);
            boolean eventoNovo = cadastroEvento != null && cadastroEvento.anterior() == null;
            if (anterior == null && !eventoNovo) {
                anterior = Map.of("id", idEvento, "parceiros", List.of());
            }
            Map<String, Object> novo = consultarParceirosAtuais(conexao, idEvento);
            if (anterior == null && ((List<?>) novo.get("parceiros")).isEmpty()) return;
            Evento dono = (Evento) evento.getAffectedOwnerOrNull();
            Map<String, Object> contexto = Map.of("titulo", dono.getTitulo());
            gravarEvento(conexao, EntidadeAuditada.EVENTO, "parceiro_evento", idEvento.toString(),
                    anterior, novo, contexto, contexto);
        });
    }

    @Override
    public void onPreUpdateCollection(PreCollectionUpdateEvent evento) {
        guardarUnidadesAnteriores(evento);
        guardarParceirosAnteriores(evento);
    }

    @Override
    public void onPreRemoveCollection(PreCollectionRemoveEvent evento) {
        guardarUnidadesAnteriores(evento);
        guardarParceirosAnteriores(evento);
    }

    @Override
    public void onPostUpdateCollection(PostCollectionUpdateEvent evento) {
        registrarMudancaDeUnidades(evento);
        registrarMudancaDeParceiros(evento);
    }

    @Override
    public void onPostRemoveCollection(PostCollectionRemoveEvent evento) {
        registrarMudancaDeUnidades(evento);
        registrarMudancaDeParceiros(evento);
    }

    @Override
    public void onPostRecreateCollection(PostCollectionRecreateEvent evento) {
        registrarMudancaDeUnidades(evento);
        registrarMudancaDeParceiros(evento);
    }
}
