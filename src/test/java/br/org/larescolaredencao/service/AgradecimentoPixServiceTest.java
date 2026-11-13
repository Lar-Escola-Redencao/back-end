package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.CodigoPixResponseDTO;

class AgradecimentoPixServiceTest {
    private final EmailService emailService = mock(EmailService.class);
    private final CaptchaPixService captcha = mock(CaptchaPixService.class);
    private final Clock relogio = mock(Clock.class);
    private final AtomicReference<Instant> agora = new AtomicReference<>(Instant.parse("2026-11-08T12:00:00Z"));
    private AgradecimentoPixService service;

    @BeforeEach
    void preparar() {
        when(relogio.instant()).thenAnswer(invocacao -> agora.get());
        service = new AgradecimentoPixService(emailService, captcha, relogio);
        registrar("LER123");
    }

    @Test
    void enviaUmaVezSomenteDepoisDeValidarOCaptcha() {
        service.agradecer("LER123", " DOADOR@example.org ", "token", "ip");
        var ordem = inOrder(captcha, emailService);
        ordem.verify(captcha).validar("token", "ip", "LER123");
        ordem.verify(emailService).enviarAgradecimentoPix("doador@example.org", BigDecimal.TEN);
        assertThatThrownBy(() -> service.agradecer("LER123", "outro@example.org", "novo", "outro-ip"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        verify(emailService, times(1)).enviarAgradecimentoPix(anyString(), any());
    }

    @Test
    void rejeitaCodigoDesconhecidoExpiradoECaptchaAusente() {
        assertThatThrownBy(() -> service.agradecer("inexistente", "a@b.org", "token", "ip"))
                .hasMessageContaining("404");
        assertThatThrownBy(() -> service.agradecer("LER123", "a@b.org", "", "ip"))
                .hasMessageContaining("400");
        avancar(901);
        assertThatThrownBy(() -> service.agradecer("LER123", "a@b.org", "token", "ip"))
                .hasMessageContaining("410");
        verifyNoInteractions(captcha, emailService);
    }

    @Test
    void naoEnviaComCaptchaInvalidoENaoReutilizaTokenNemEmOutroCodigo() {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verificação inválida"))
                .when(captcha).validar("token", "ip", "LER123");
        assertThatThrownBy(() -> service.agradecer("LER123", "a@b.org", "token", "ip"))
                .isInstanceOf(BloqueioAgradecimentoException.class).hasMessageContaining("400");
        registrar("LER456");
        assertThatThrownBy(() -> service.agradecer("LER456", "outro@b.org", "token", "outro-ip"))
                .hasMessageContaining("já foi utilizada");
        verifyNoInteractions(emailService);
    }

    @Test
    void falhaDeEmailExigeEsperaETokenNovoAntesDeNovaTentativa() {
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Falha no envio"))
                .doNothing().when(emailService).enviarAgradecimentoPix("a@b.org", BigDecimal.TEN);
        assertThatThrownBy(() -> service.agradecer("LER123", "a@b.org", "token", "ip"))
                .isInstanceOfSatisfying(BloqueioAgradecimentoException.class,
                        erro -> assertThat(erro.getRetryAfterSeconds()).isEqualTo(60));
        assertThatThrownBy(() -> service.agradecer("LER123", "outro@b.org", "novo", "outro-ip"))
                .hasMessageContaining("429");
        avancar(60);
        service.agradecer("LER123", "a@b.org", "novo", "ip");
        verify(emailService, times(2)).enviarAgradecimentoPix("a@b.org", BigDecimal.TEN);
    }

    @Test
    void limitaDestinatarioNormalizadoMesmoTrocandoCodigoEIp() {
        service.agradecer("LER123", "A@B.ORG", "token", "ip");
        registrar("LER456");
        avancar(20);
        assertThatThrownBy(() -> service.agradecer("LER456", " a@b.org ", "novo", "outro-ip"))
                .isInstanceOfSatisfying(BloqueioAgradecimentoException.class,
                        erro -> assertThat(erro.getRetryAfterSeconds()).isEqualTo(40));
        avancar(40);
        service.agradecer("LER456", "a@b.org", "novo", "outro-ip");
    }

    @Test
    void permitePessoasDiferentesNoMesmoIpMasLimitaDezPorMinuto() {
        for (int i = 0; i < 10; i++) {
            registrar("LER" + i);
            service.agradecer("LER" + i, "pessoa" + i + "@b.org", "token" + i, "ip");
        }
        registrar("LER11");
        assertThatThrownBy(() -> service.agradecer("LER11", "outra@b.org", "novo", "ip"))
                .hasMessageContaining("429");
        avancar(60);
        service.agradecer("LER11", "outra@b.org", "novo", "ip");
    }

    @Test
    void limitaCincoTentativasPorHoraPorDestinatario() {
        for (int i = 0; i < 5; i++) {
            registrar("LER" + i);
            service.agradecer("LER" + i, "a@b.org", "token" + i, "ip" + i);
            avancar(60);
        }
        registrar("LER6");
        assertThatThrownBy(() -> service.agradecer("LER6", "a@b.org", "novo", "outro-ip"))
                .isInstanceOfSatisfying(BloqueioAgradecimentoException.class,
                        erro -> assertThat(erro.getRetryAfterSeconds()).isEqualTo(3300));
    }

    @Test
    void limitaCemTentativasPorHoraPorIp() {
        for (int i = 0; i < 100; i++) {
            registrar("LER" + i);
            service.agradecer("LER" + i, "pessoa" + i + "@b.org", "token" + i, "ip");
            if (i % 10 == 9) avancar(60);
        }
        registrar("LER101");
        assertThatThrownBy(() -> service.agradecer("LER101", "outra@b.org", "novo", "ip"))
                .hasMessageContaining("429");
    }

    @Test
    void reservaCodigoAntesDaChamadaExternaERecusaConcorrencia() throws Exception {
        CountDownLatch entrou = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        doAnswer(invocacao -> {
            entrou.countDown();
            assertThat(liberar.await(3, TimeUnit.SECONDS)).isTrue();
            return null;
        }).when(captcha).validar("token", "ip", "LER123");
        CompletableFuture<Void> primeiro = CompletableFuture.runAsync(
                () -> service.agradecer("LER123", "a@b.org", "token", "ip"));
        try {
            assertThat(entrou.await(3, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> service.agradecer("LER123", "outro@b.org", "novo", "outro-ip"))
                    .hasMessageContaining("429");
            verifyNoInteractions(emailService);
        } finally {
            liberar.countDown();
        }
        primeiro.get(3, TimeUnit.SECONDS);
        verify(emailService, times(1)).enviarAgradecimentoPix("a@b.org", BigDecimal.TEN);
    }

    @Test
    void falhaDoProvedorTambemReservaLimitesENaoChamaEmail() {
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Verificação indisponível"))
                .when(captcha).validar("token", "ip", "LER123");
        assertThatThrownBy(() -> service.agradecer("LER123", "a@b.org", "token", "ip"))
                .hasMessageContaining("503");
        assertThatThrownBy(() -> service.agradecer("LER123", "a@b.org", "novo", "ip"))
                .hasMessageContaining("429");
        verifyNoInteractions(emailService);
    }

    private void avancar(long segundos) { agora.updateAndGet(instante -> instante.plusSeconds(segundos)); }

    private void registrar(String id) {
        OffsetDateTime geradoEm = agora.get().atOffset(ZoneOffset.UTC);
        service.registrar(new CodigoPixResponseDTO(id, BigDecimal.TEN, "doacoes@example.org",
                "LAR ESCOLA REDENCAO", "ARARAQUARA", "payload", "imagem", geradoEm, geradoEm.plusMinutes(15)));
    }
}
