package br.org.larescolaredencao.validacao;

import br.org.larescolaredencao.dto.*;
import br.org.larescolaredencao.model.Evento;
import br.org.larescolaredencao.model.Parceiro;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LimitesConteudoTest {
    @Test void parceiroAceitaNomeExtensoNoCadastroEdicaoEEntidade() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (var tipo : new Class<?>[]{CriarParceiroDTO.class, AtualizarParceiroDTO.class, Parceiro.class}) {
                assertThat(validator.validateValue(tipo, "nome",
                        "Secretaria de Desenvolvimento Social do Estado de São Paulo (SEDS)")).isEmpty();
                assertThat(validator.validateValue(tipo, "nome", "a".repeat(255))).isEmpty();
                assertThat(validator.validateValue(tipo, "nome", "a".repeat(256))).isNotEmpty();
            }
        }
    }

    @Test void descricaoDeEventoAceitaDezMilCaracteresNoCadastroEdicaoEEntidade() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (var tipo : new Class<?>[]{CriarEventoDTO.class, AtualizarEventoDTO.class, Evento.class}) {
                assertThat(validator.validateValue(tipo, "descricao", "a".repeat(10000))).isEmpty();
                assertThat(validator.validateValue(tipo, "descricao", "a".repeat(10001))).isNotEmpty();
            }
        }
    }
}
