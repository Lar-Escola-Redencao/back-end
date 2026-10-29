package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.Ocorrencia;
import br.org.larescolaredencao.model.enums.TipoOcorrencia;
import java.time.LocalDateTime;

public record OcorrenciaAcompanhamentoDTO(Integer id, Integer idMatricula,
        LocalDateTime dataOcorrencia, LocalDateTime dataCriacao, String descricao,
        TipoOcorrencia tipoOcorrencia, String nomeMembro, Integer idMembro, String turmaUnidade) {
    public static OcorrenciaAcompanhamentoDTO de(Ocorrencia o) {
        var turma = o.getMatricula().getTurma();
        String periodo = switch (turma.getPeriodo()) {
            case MANHA -> "Manhã";
            case TARDE -> "Tarde";
        };
        return new OcorrenciaAcompanhamentoDTO(o.getId(), o.getMatricula().getId(), o.getDataOcorrencia(),
                o.getDataCriacao(), o.getDescricao(), o.getTipoOcorrencia(), o.getMembro().getNomeCompleto(),
                o.getMembro().getId(), periodo + " — " + turma.getUnidade().getNome());
    }
}
