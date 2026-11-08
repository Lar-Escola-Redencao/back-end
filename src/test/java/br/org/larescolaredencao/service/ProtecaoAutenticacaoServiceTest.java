package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ProtecaoAutenticacaoServiceTest {
    private final CaptchaPixService captcha = mock(CaptchaPixService.class);
    private final Relogio relogio = new Relogio();
    private ProtecaoAutenticacaoService protecao;

    @BeforeEach
    void preparar() { protecao = new ProtecaoAutenticacaoService(captcha, relogio); }

    @Test
    void exigeCaptchaAposCincoFalhasSemDependerDoFront() {
        for (int i = 0; i < 5; i++) {
            var tentativa = protecao.iniciarLogin("Pessoa@exemplo.com", null, "ip");
            assertThat(tentativa.captchaObrigatorio()).isFalse();
            assertThat(protecao.falhouLogin(tentativa)).isEqualTo(i == 4);
            protecao.concluir(tentativa, false);
        }
        var erro = assertThrows(ProtecaoAutenticacaoException.class,
                () -> protecao.iniciarLogin("pessoa@exemplo.com", null, "outro-ip"));
        assertThat(erro.isCaptchaRequired()).isTrue();
        assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(captcha);
    }

    @Test
    void falhasDoMesmoIpExigemCaptchaMesmoAlternandoContas() {
        for (int i = 0; i < 20; i++) {
            var tentativa = protecao.iniciarLogin("pessoa" + i + "@exemplo.com", null, "ip");
            protecao.falhouLogin(tentativa);
            protecao.concluir(tentativa, false);
        }
        assertThrows(ProtecaoAutenticacaoException.class,
                () -> protecao.iniciarLogin("nova@exemplo.com", null, "ip"));
        assertThat(protecao.iniciarLogin("nova@exemplo.com", null, "outro-ip").captchaObrigatorio()).isFalse();
    }

    @Test
    void chamadasSimultaneasNaoContornamOLimiar() {
        for (int i = 0; i < 5; i++) protecao.iniciarLogin("pessoa@exemplo.com", null, "ip");
        assertThrows(ProtecaoAutenticacaoException.class,
                () -> protecao.iniciarLogin("pessoa@exemplo.com", null, "ip"));
    }

    @Test
    void falhasExpiramEmQuinzeMinutos() {
        falharCincoVezes();
        relogio.avancar(900);
        assertThat(protecao.iniciarLogin("pessoa@exemplo.com", null, "ip").captchaObrigatorio()).isFalse();
    }

    @Test
    void loginValidoLimpaFalhasDaConta() {
        falharCincoVezes();
        var tentativa = protecao.iniciarLogin("pessoa@exemplo.com", "token", "ip");
        verify(captcha).validar("token", "ip", "login", "login");
        protecao.concluir(tentativa, true);
        assertThat(protecao.iniciarLogin("pessoa@exemplo.com", null, "ip").captchaObrigatorio()).isFalse();
    }

    @Test
    void limitaLoginMesmoComCaptchaValidoERetornaEspera() {
        for (int i = 0; i < 10; i++) {
            var tentativa = protecao.iniciarLogin("pessoa@exemplo.com", null, "ip");
            protecao.concluir(tentativa, true);
        }
        var erro = assertThrows(ProtecaoAutenticacaoException.class,
                () -> protecao.iniciarLogin("pessoa@exemplo.com", "token", "ip"));
        assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(erro.getRetryAfterSeconds()).isEqualTo(60);
        relogio.avancar(60);
        assertThat(protecao.iniciarLogin("pessoa@exemplo.com", null, "ip")).isNotNull();
    }

    @Test
    void recuperacaoSempreExigeCaptcha() {
        assertThrows(ProtecaoAutenticacaoException.class,
                () -> protecao.iniciarRecuperacao("pessoa@exemplo.com", null, "ip"));
        verifyNoInteractions(captcha);
    }

    @Test
    void recuperacaoValidaContextoEPermiteOutraPessoaNoMesmoIp() {
        var tentativa = protecao.iniciarRecuperacao("pessoa@exemplo.com", "token", "ip");
        protecao.concluir(tentativa, false);
        verify(captcha).validar("token", "ip", "recuperacao_senha", "recuperacao_senha");
        var erro = assertThrows(ProtecaoAutenticacaoException.class,
                () -> protecao.iniciarRecuperacao("PESSOA@exemplo.com", "outro", "outro-ip"));
        assertThat(erro.getRetryAfterSeconds()).isEqualTo(60);
        assertThat(protecao.iniciarRecuperacao("outra@exemplo.com", "outro", "ip")).isNotNull();
    }

    @Test
    void naoPermiteReutilizarTokenNemEmOutraConta() {
        var tentativa = protecao.iniciarRecuperacao("pessoa@exemplo.com", "token", "ip");
        protecao.concluir(tentativa, false);
        assertThrows(ProtecaoAutenticacaoException.class,
                () -> protecao.iniciarRecuperacao("outra@exemplo.com", "token", "outro-ip"));
        verify(captcha, times(1)).validar(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void indisponibilidadePermiteNovaTentativaControlada() {
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)).when(captcha)
                .validar("falha", "ip", "recuperacao_senha", "recuperacao_senha");
        var erro = assertThrows(ProtecaoAutenticacaoException.class,
                () -> protecao.iniciarRecuperacao("pessoa@exemplo.com", "falha", "ip"));
        assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(erro.getRetryAfterSeconds()).isEqualTo(60);
        relogio.avancar(60);
        assertThat(protecao.iniciarRecuperacao("pessoa@exemplo.com", "novo", "ip")).isNotNull();
    }

    @Test
    void reservaAntesDaVerificacaoParaBloquearRecuperacaoConcorrente() throws Exception {
        CountDownLatch entrou = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        doAnswer(invocacao -> {
            entrou.countDown();
            if (!liberar.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout");
            return null;
        }).when(captcha).validar("primeiro", "ip", "recuperacao_senha", "recuperacao_senha");
        try (var executor = Executors.newSingleThreadExecutor()) {
            var primeira = executor.submit(() -> protecao.iniciarRecuperacao("pessoa@exemplo.com", "primeiro", "ip"));
            try {
                assertThat(entrou.await(5, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> protecao.iniciarRecuperacao("pessoa@exemplo.com", "segundo", "ip"))
                        .isInstanceOf(ProtecaoAutenticacaoException.class);
                verify(captcha, never()).validar("segundo", "ip", "recuperacao_senha", "recuperacao_senha");
            } finally { liberar.countDown(); }
            protecao.concluir(primeira.get(5, TimeUnit.SECONDS), false);
        }
    }

    private void falharCincoVezes() {
        for (int i = 0; i < 5; i++) {
            var tentativa = protecao.iniciarLogin("pessoa@exemplo.com", null, "ip");
            protecao.falhouLogin(tentativa);
            protecao.concluir(tentativa, false);
        }
    }

    private static class Relogio extends Clock {
        private Instant agora = Instant.parse("2026-11-08T12:00:00Z");
        void avancar(long segundos) { agora = agora.plusSeconds(segundos); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    }
}
