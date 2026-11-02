package br.org.larescolaredencao.model.enums;

import java.util.List;

public enum AreaSistemaHistorico {
    GESTAO_INTERNA(List.of(
            TipoRegistroHistorico.COLABORADORES, TipoRegistroHistorico.USUARIOS,
            TipoRegistroHistorico.CONTATOS, TipoRegistroHistorico.VINCULOS_CONTATOS,
            TipoRegistroHistorico.PRESENCAS, TipoRegistroHistorico.OCORRENCIAS,
            TipoRegistroHistorico.UNIDADES, TipoRegistroHistorico.TURMAS)),
    CONTEUDO_PUBLICO(List.of(
            TipoRegistroHistorico.EVENTOS, TipoRegistroHistorico.DIRETORIA,
            TipoRegistroHistorico.PARCEIROS, TipoRegistroHistorico.REDES_SOCIAIS,
            TipoRegistroHistorico.TRANSPARENCIA, TipoRegistroHistorico.CONHECA_OSC,
            TipoRegistroHistorico.GRAFICA, TipoRegistroHistorico.PIX));

    private final List<TipoRegistroHistorico> tipos;

    AreaSistemaHistorico(List<TipoRegistroHistorico> tipos) {
        this.tipos = tipos;
    }

    public List<TipoRegistroHistorico> tipos() {
        return tipos;
    }
}
