package br.org.larescolaredencao.dto;

import java.util.List;
import java.util.Map;

public record HistoricoDetalheDTO(
        HistoricoEventoDTO evento,
        String identificadorEspecifico,
        boolean referenciaAnteriorDisponivel,
        String observacao,
        Map<String, Object> contextoAnterior,
        Map<String, Object> contextoNovo,
        List<CampoAlteradoDTO> campos) {

    public record CampoAlteradoDTO(
            String campo,
            String nome,
            Object valorAnterior,
            Object valorNovo,
            boolean anteriorConhecido) {
    }
}
