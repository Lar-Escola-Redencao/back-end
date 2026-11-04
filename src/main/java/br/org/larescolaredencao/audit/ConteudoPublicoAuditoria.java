package br.org.larescolaredencao.audit;

import br.org.larescolaredencao.model.enums.TipoRegistroHistorico;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConteudoPublicoAuditoria {

    private ConteudoPublicoAuditoria() {
    }

    static Map<String, Object> montarContexto(Connection conexao, EntidadeAuditada entidade,
                                             Map<String, Object> valores) throws SQLException {
        Map<String, Object> contexto = new LinkedHashMap<>();
        if (entidade == EntidadeAuditada.LINK_EVENTO) {
            contexto.put("evento", consultar(conexao, "select titulo from evento where id = ?", valores.get("idEvento")));
            contexto.put("redeSocial", consultar(conexao, "select nome from rede_social where id = ?", valores.get("idRedeSocial")));
        }

        Object idPagina = null;
        if (entidade == EntidadeAuditada.PAGINA) {
            idPagina = valores.get("id");
        } else if (entidade == EntidadeAuditada.SECAO) {
            idPagina = valores.get("pagina");
        } else if (entidade == EntidadeAuditada.DOCUMENTO) {
            Map<String, Object> secao = consultar(conexao,
                    "select titulo, id_pagina from secao where id = ?", valores.get("secao"));
            idPagina = secao.get("id_pagina");
            contexto.put("secao", secao);
        }

        if (idPagina != null) {
            contexto.put("idPagina", idPagina);
            contexto.put("pagina", consultar(conexao, "select nome from pagina where id = ?", idPagina));
        }
        return contexto;
    }

    static TipoRegistroHistorico categoria(EntidadeAuditada entidade, Map<String, Object> contexto) {
        if (entidade != EntidadeAuditada.PAGINA && entidade != EntidadeAuditada.SECAO
                && entidade != EntidadeAuditada.DOCUMENTO) {
            return entidade.categoria;
        }
        if (!(contexto.get("idPagina") instanceof Number pagina)) {
            throw new IllegalStateException("Não foi possível identificar a página do conteúdo auditado.");
        }
        return switch (pagina.intValue()) {
            case 1 -> TipoRegistroHistorico.TRANSPARENCIA;
            case 2 -> TipoRegistroHistorico.CONHECA_OSC;
            case 3 -> TipoRegistroHistorico.GRAFICA;
            case 4 -> TipoRegistroHistorico.PIX;
            default -> throw new IllegalStateException("Página não contemplada na auditoria do conteúdo público.");
        };
    }

    private static Map<String, Object> consultar(Connection conexao, String sql, Object id) throws SQLException {
        Map<String, Object> valores = new LinkedHashMap<>();
        try (var consulta = conexao.prepareStatement(sql)) {
            consulta.setObject(1, id);
            try (var resultado = consulta.executeQuery()) {
                if (resultado.next()) {
                    var colunas = resultado.getMetaData();
                    for (int indice = 1; indice <= colunas.getColumnCount(); indice++) {
                        valores.put(colunas.getColumnLabel(indice), resultado.getObject(indice));
                    }
                }
            }
        }
        return valores;
    }
}
