package br.org.larescolaredencao.audit;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ReferenciaInicialAuditoria implements ApplicationRunner {

    private final DataSource dataSource;

    public ReferenciaInicialAuditoria(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments argumentos) throws SQLException {
        try (Connection conexao = dataSource.getConnection()) {
            conexao.setAutoCommit(false);
            String nomeBloqueio = "ler_auditoria_" + conexao.getCatalog();
            adquirirBloqueio(conexao, nomeBloqueio);

            try {
                registrarReferenciasAusentes(conexao);
                conexao.commit();
            } catch (SQLException | RuntimeException excecao) {
                conexao.rollback();
                throw excecao;
            } finally {
                liberarBloqueio(conexao, nomeBloqueio);
            }
        }
    }

    private void registrarReferenciasAusentes(Connection conexao) throws SQLException {
        Integer idRevisao = null;

        for (EntidadeAuditada entidade : EntidadeAuditada.values()) {
            String registrosSemReferencia = montarSelecaoSemReferencia(entidade);
            if (!possuiRegistrosSemReferencia(conexao, registrosSemReferencia)) {
                continue;
            }

            if (idRevisao == null) {
                idRevisao = criarRevisaoDeImplantacao(conexao);
            }

            copiarEstadoInicial(conexao, entidade, registrosSemReferencia, idRevisao);
            if (entidade == EntidadeAuditada.MEMBRO) {
                copiarVinculosDeUnidades(conexao, idRevisao);
            }
        }
    }

    private String montarSelecaoSemReferencia(EntidadeAuditada entidade) {
        String comparacaoChaves = entidade.chaves.stream()
                .map(chave -> "historico." + chave + " = registro." + chave)
                .collect(Collectors.joining(" and "));

        return " from " + entidade.tabela + " registro"
                + " where not exists (select 1 from " + entidade.tabela + "_aud historico"
                + " where " + comparacaoChaves + ")";
    }

    private boolean possuiRegistrosSemReferencia(Connection conexao, String selecao) throws SQLException {
        try (var consulta = conexao.createStatement();
             var resultado = consulta.executeQuery("select exists(select 1" + selecao + ")")) {
            resultado.next();
            return resultado.getBoolean(1);
        }
    }

    private int criarRevisaoDeImplantacao(Connection conexao) throws SQLException {
        String sql = """
                insert into revinfo(revtstmp, referencia_implantacao)
                values (?, true)
                """;

        try (var comando = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            comando.setLong(1, System.currentTimeMillis());
            comando.executeUpdate();

            try (var chaves = comando.getGeneratedKeys()) {
                if (!chaves.next()) {
                    throw new SQLException("Não foi possível identificar a revisão de implantação.");
                }
                return chaves.getInt(1);
            }
        }
    }

    private void copiarEstadoInicial(
            Connection conexao,
            EntidadeAuditada entidade,
            String selecao,
            int idRevisao) throws SQLException {

        String tabelaHistorico = entidade.tabela + "_aud";
        List<String> colunas = consultarColunasAuditadas(conexao, tabelaHistorico);
        String colunasDestino = String.join(", ", colunas);
        String colunasOrigem = colunas.stream()
                .map(coluna -> "registro." + coluna)
                .collect(Collectors.joining(", "));

        String sql = "insert into " + tabelaHistorico + "(" + colunasDestino + ", rev, revtype)"
                + " select " + colunasOrigem + ", " + idRevisao + ", 0" + selecao;

        try (var comando = conexao.createStatement()) {
            comando.executeUpdate(sql);
        }
    }

    private List<String> consultarColunasAuditadas(Connection conexao, String tabela) throws SQLException {
        List<String> colunas = new ArrayList<>();

        try (var consulta = conexao.createStatement();
             var resultado = consulta.executeQuery("select * from " + tabela + " where 1 = 0")) {
            var metadados = resultado.getMetaData();

            for (int indice = 1; indice <= metadados.getColumnCount(); indice++) {
                String coluna = metadados.getColumnLabel(indice);
                if (!coluna.equals("rev") && !coluna.equals("revtype")) {
                    colunas.add(coluna);
                }
            }
        }

        return colunas;
    }

    private void copiarVinculosDeUnidades(Connection conexao, int idRevisao) throws SQLException {
        String sql = """
                insert into membro_unidade_aud(id_membro, id_unidade, rev, revtype)
                select vinculo.id_membro, vinculo.id_unidade, ?, 0
                from membro_unidade vinculo
                join membro_aud membro on membro.id = vinculo.id_membro
                where membro.rev = ?
                """;

        try (var comando = conexao.prepareStatement(sql)) {
            comando.setInt(1, idRevisao);
            comando.setInt(2, idRevisao);
            comando.executeUpdate();
        }
    }

    private void adquirirBloqueio(Connection conexao, String nome) throws SQLException {
        try (var comando = conexao.prepareStatement("select get_lock(?, 30)")) {
            comando.setString(1, nome);

            try (var resultado = comando.executeQuery()) {
                if (!resultado.next() || resultado.getInt(1) != 1) {
                    throw new IllegalStateException("Não foi possível bloquear a preparação da auditoria.");
                }
            }
        }
    }

    private void liberarBloqueio(Connection conexao, String nome) throws SQLException {
        try (var comando = conexao.prepareStatement("select release_lock(?)")) {
            comando.setString(1, nome);
            comando.executeQuery().close();
        }
    }
}
