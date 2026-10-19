package br.org.larescolaredencao.service;

import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Papel;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class PeriodoDiarioTest {
    private Membro membro(String perfil) {
        Papel papel = new Papel();
        papel.setNomePapel(perfil);
        Membro membro = new Membro();
        membro.setPapel(papel);
        return membro;
    }

    @Test
    void permiteLimiteInclusivoEBloqueiaDiaAnterior() {
        LocalDate hoje = LocalDate.of(2026, 10, 17);
        Membro monitor = membro("MONITOR");
        assertThatCode(() -> PeriodoDiario.validar(monitor, LocalDate.of(2026, 9, 1), hoje)).doesNotThrowAnyException();
        assertThatThrownBy(() -> PeriodoDiario.validar(monitor, LocalDate.of(2026, 8, 31), hoje))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void trataViradaDeAnoEMesBissexto() {
        Membro monitor = membro("MONITOR");
        assertThatCode(() -> PeriodoDiario.validar(monitor, LocalDate.of(2025, 12, 1), LocalDate.of(2026, 1, 31)))
                .doesNotThrowAnyException();
        assertThatCode(() -> PeriodoDiario.validar(monitor, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 31)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> PeriodoDiario.validar(monitor, LocalDate.of(2024, 1, 31), LocalDate.of(2024, 3, 31)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void preservaPermissoesDeCoordenadorEAdministrador() {
        for (String perfil : new String[] { "ADMINISTRADOR", "COORDENADOR" }) {
            assertThatCode(() -> PeriodoDiario.validar(membro(perfil), LocalDate.of(2020, 1, 1), LocalDate.of(2026, 10, 17)))
                    .doesNotThrowAnyException();
        }
    }
}
