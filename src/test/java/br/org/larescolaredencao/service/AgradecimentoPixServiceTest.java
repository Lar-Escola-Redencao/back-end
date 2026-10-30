package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.CodigoPixResponseDTO;

class AgradecimentoPixServiceTest {

    private final EmailService emailService = mock(EmailService.class);
    private final AgradecimentoPixService service = new AgradecimentoPixService(emailService);

    @Test
    void deveEnviarUmAgradecimentoPorCodigoComValorGeradoPeloServidor() {
        service.registrar(codigo(OffsetDateTime.now().plusMinutes(15)));
        service.agradecer("LER123", " doador@example.org ");
        verify(emailService).enviarAgradecimentoPix("doador@example.org", BigDecimal.TEN);
        assertThatThrownBy(() -> service.agradecer("LER123", "outro@example.org"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
    }

    @Test
    void deveRecusarCodigoDesconhecidoSemEnviarEmail() {
        assertThatThrownBy(() -> service.agradecer("inexistente", "doador@example.org"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        verifyNoInteractions(emailService);
    }

    @Test
    void deveRecusarSessaoExpiradaSemEnviarEmail() {
        service.registrar(codigo(OffsetDateTime.now().minusSeconds(1)));
        assertThatThrownBy(() -> service.agradecer("LER123", "doador@example.org"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("410");
        verifyNoInteractions(emailService);
    }

    @Test
    void devePermitirNovaTentativaQuandoOSmtpFalha() {
        service.registrar(codigo(OffsetDateTime.now().plusMinutes(15)));
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE))
                .doNothing().when(emailService).enviarAgradecimentoPix("doador@example.org", BigDecimal.TEN);
        assertThatThrownBy(() -> service.agradecer("LER123", "doador@example.org"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("503");
        service.agradecer("LER123", "doador@example.org");
        verify(emailService, times(2)).enviarAgradecimentoPix("doador@example.org", BigDecimal.TEN);
    }

    private CodigoPixResponseDTO codigo(OffsetDateTime expiraEm) {
        return new CodigoPixResponseDTO("LER123", BigDecimal.TEN, "doacoes@example.org",
                "LAR ESCOLA REDENCAO", "ARARAQUARA", "payload", "imagem", expiraEm.minusMinutes(15), expiraEm);
    }
}
