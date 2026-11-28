package br.org.larescolaredencao.validacao;

import br.org.larescolaredencao.dto.*;
import br.org.larescolaredencao.model.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TextosLongosTest {
    @Test void camposLongosAceitamDezMilERejeitamAcimaDisso() {
        Object[][] campos = {
            {EntrevistaDTO.class, "relato"}, {EntrevistaSocial.class, "relato"},
            {CriarOcorrenciaDTO.class, "descricao"}, {AtualizarOcorrenciaDTO.class, "descricao"},
            {Ocorrencia.class, "descricao"}, {InativarUsuarioDTO.class, "justificativa"},
            {Matricula.class, "justificativaEgresso"}, {Secao.class, "conteudo"},
            {CriarEventoDTO.class, "descricao"}, {AtualizarEventoDTO.class, "descricao"},
            {Evento.class, "descricao"},
        };
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (var campo : campos) {
                var tipo = (Class<?>) campo[0]; var nome = (String) campo[1];
                assertThat(validator.validateValue(tipo, nome, "a".repeat(10000)))
                        .as("%s.%s no limite", tipo.getSimpleName(), nome).isEmpty();
                assertThat(validator.validateValue(tipo, nome, "a".repeat(10001)))
                        .as("%s.%s acima do limite", tipo.getSimpleName(), nome).isNotEmpty();
            }
        }
    }
}
