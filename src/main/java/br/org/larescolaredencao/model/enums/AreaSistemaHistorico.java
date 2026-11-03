package br.org.larescolaredencao.model.enums;

import java.util.List;

public enum AreaSistemaHistorico {
    GESTAO_INTERNA(List.of(
            TipoRegistroHistorico.COLABORADORES, TipoRegistroHistorico.USUARIOS,
            TipoRegistroHistorico.CONTATOS, TipoRegistroHistorico.VINCULOS_CONTATOS,
            TipoRegistroHistorico.PRESENCAS, TipoRegistroHistorico.OCORRENCIAS, TipoRegistroHistorico.ENTREVISTAS,
            TipoRegistroHistorico.UNIDADES, TipoRegistroHistorico.TURMAS)),
    USUARIOS_E_ACOMPANHAMENTO(List.of(
            TipoRegistroHistorico.USUARIOS,
            TipoRegistroHistorico.VINCULOS_CONTATOS, TipoRegistroHistorico.PRESENCAS,
            TipoRegistroHistorico.OCORRENCIAS, TipoRegistroHistorico.ENTREVISTAS)),
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

    public List<TipoRegistroHistorico> tiposNoContexto(Integer idUsuario) {
        if (this != USUARIOS_E_ACOMPANHAMENTO || idUsuario == null) return tipos;
        // No perfil, preservar também as alterações dos contatos vinculados ao usuário.
        return java.util.stream.Stream.concat(tipos.stream(), java.util.stream.Stream.of(TipoRegistroHistorico.CONTATOS))
                .toList();
    }
}
