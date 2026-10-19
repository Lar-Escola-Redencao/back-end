package br.org.larescolaredencao.service;

import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.enums.Perfil;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class PeriodoDiario {
    private PeriodoDiario() {}

    static void validar(Membro membro, LocalDate dataRegistro, LocalDate hoje) {
        LocalDate limite = hoje.withDayOfMonth(1).minusMonths(1);
        if (Perfil.fromNomePapel(membro.getPapel().getNomePapel()) == Perfil.MONITOR
                && dataRegistro.isBefore(limite)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Monitores não podem cadastrar registros anteriores ao primeiro dia do mês passado.");
        }
    }
}
