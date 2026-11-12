package br.org.larescolaredencao.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.api.GlobalExceptionHandler;
import br.org.larescolaredencao.api.PixController;
import br.org.larescolaredencao.dto.CodigoPixResponseDTO;

/**
 * Fluxo HTTP completo do agradecimento por e-mail: controller, bloqueios e validação do CAPTCHA reais,
 * com o Turnstile simulado e o envio de e-mail mockado. Cobre sucesso, CAPTCHA expirado/inválido,
 * bloqueio temporário, falha de comunicação e conclusão sem e-mail.
 */
class AgradecimentoPixFluxoHttpTest {
    private static final String SITEVERIFY = "https://captcha.example/siteverify";
    private static final String CORPO = "{\"email\":\"doador@example.org\",\"captchaToken\":\"%s\"}";

    private final AtomicReference<Instant> agora = new AtomicReference<>(Instant.parse("2026-11-08T12:00:00Z"));
    private final Clock relogio = mock(Clock.class);
    private final EmailService email = mock(EmailService.class);
    private MockRestServiceServer turnstile;
    private AgradecimentoPixService agradecimento;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        when(relogio.instant()).thenAnswer(invocacao -> agora.get());
        RestClient.Builder builder = RestClient.builder().baseUrl(SITEVERIFY);
        turnstile = MockRestServiceServer.bindTo(builder).build();
        var captcha = new CaptchaPixService(builder.build(), "segredo", "doar.example.org", relogio);
        agradecimento = new AgradecimentoPixService(email, captcha, relogio);
        mvc = MockMvcBuilders.standaloneSetup(new PixController(mock(PixService.class), agradecimento))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        registrar("LER123");
    }

    @Test
    void envioBemSucedidoRetorna204EEnviaUmaVez() throws Exception {
        turnstile.expect(requestTo(SITEVERIFY)).andRespond(withSuccess(sucesso("LER123", -60), MediaType.APPLICATION_JSON));
        enviar("LER123", "token").andExpect(status().isNoContent());
        verify(email).enviarAgradecimentoPix("doador@example.org", BigDecimal.TEN);
        turnstile.verify();

        // Reenvio do mesmo Pix não gera outro e-mail.
        enviar("LER123", "outro-token").andExpect(status().isConflict());
        verify(email, times(1)).enviarAgradecimentoPix(anyString(), any());
    }

    @Test
    void captchaInvalidoRetorna400SemEnviarEmail() throws Exception {
        turnstile.expect(requestTo(SITEVERIFY)).andRespond(withSuccess(
                "{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}", MediaType.APPLICATION_JSON));
        enviar("LER123", "token-falso")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A verificação expirou ou não é válida. Faça uma nova verificação e tente novamente."))
                .andExpect(jsonPath("$.retryAfterSeconds").value(60));
        verifyNoInteractions(email);
    }

    @Test
    void captchaExpiradoOuJaUsadoRetorna400SemEnviarEmail() throws Exception {
        turnstile.expect(requestTo(SITEVERIFY)).andRespond(withSuccess(
                "{\"success\":false,\"error-codes\":[\"timeout-or-duplicate\"]}", MediaType.APPLICATION_JSON));
        enviar("LER123", "token-expirado").andExpect(status().isBadRequest());
        verifyNoInteractions(email);
    }

    @Test
    void captchaResolvidoHaMaisDeCincoMinutosEhRecusado() throws Exception {
        turnstile.expect(requestTo(SITEVERIFY)).andRespond(withSuccess(sucesso("LER123", -301), MediaType.APPLICATION_JSON));
        enviar("LER123", "token-antigo").andExpect(status().isBadRequest());
        verifyNoInteractions(email);
    }

    @Test
    void captchaDeOutroPixOuHostEhRecusado() throws Exception {
        turnstile.expect(requestTo(SITEVERIFY)).andRespond(withSuccess(sucesso("LER999", -10), MediaType.APPLICATION_JSON));
        enviar("LER123", "token-outro-pix").andExpect(status().isBadRequest());
        verifyNoInteractions(email);
    }

    @Test
    void tokenJaUtilizadoNaoConsultaOProvedorNovamente() throws Exception {
        turnstile.expect(requestTo(SITEVERIFY)).andRespond(withSuccess(
                "{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}", MediaType.APPLICATION_JSON));
        enviar("LER123", "mesmo-token").andExpect(status().isBadRequest());
        avancar(60);
        enviar("LER123", "mesmo-token")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Esta verificação já foi utilizada. Faça uma nova verificação."));
        turnstile.verify();
        verifyNoInteractions(email);
    }

    @Test
    void bloqueioTemporarioInformaEsperaELiberaDepoisDoPrazo() throws Exception {
        turnstile.expect(requestTo(SITEVERIFY)).andRespond(withSuccess(
                "{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}", MediaType.APPLICATION_JSON));
        turnstile.expect(requestTo(SITEVERIFY)).andRespond(withSuccess(sucesso("LER123", 55), MediaType.APPLICATION_JSON));
        enviar("LER123", "token-1").andExpect(status().isBadRequest());

        avancar(20);
        enviar("LER123", "token-2")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "40"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(40));
        verifyNoInteractions(email);

        avancar(40);
        enviar("LER123", "token-3").andExpect(status().isNoContent());
        verify(email).enviarAgradecimentoPix("doador@example.org", BigDecimal.TEN);
    }

    @Test
    void mesmoDestinatarioEmOutroPixFicaBloqueadoPorUmMinuto() throws Exception {
        turnstile.expect(requestTo(SITEVERIFY)).andRespond(withSuccess(sucesso("LER123", -5), MediaType.APPLICATION_JSON));
        enviar("LER123", "token-1").andExpect(status().isNoContent());
        registrar("LER456");
        avancar(10);
        enviar("LER456", "token-2")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "50"));
        verify(email, times(1)).enviarAgradecimentoPix(anyString(), any());
    }

    @Test
    void provedorForaDoArRetorna503ComOpcaoSemEmail() throws Exception {
        turnstile.expect(requestTo(SITEVERIFY)).andRespond(withServerError());
        enviar("LER123", "token")
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.message").value("A verificação está indisponível. Aguarde e tente novamente ou conclua sem e-mail."));
        verifyNoInteractions(email);
    }

    @Test
    void semConexaoComOProvedorRetorna503() throws Exception {
        turnstile.expect(requestTo(SITEVERIFY)).andRespond(request -> { throw new IOException("sem rede"); });
        enviar("LER123", "token")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.retryAfterSeconds").value(60));
        verifyNoInteractions(email);
    }

    @Test
    void erroDeConfiguracaoDoProvedorRetorna503EmVezDeCulparOUsuario() throws Exception {
        turnstile.expect(requestTo(SITEVERIFY)).andRespond(withSuccess(
                "{\"success\":false,\"error-codes\":[\"invalid-input-secret\"]}", MediaType.APPLICATION_JSON));
        enviar("LER123", "token").andExpect(status().isServiceUnavailable());
        verifyNoInteractions(email);
    }

    @Test
    void falhaNoEnvioDoEmailRetorna503EExigeEsperaAntesDeNovaTentativa() throws Exception {
        turnstile.expect(requestTo(SITEVERIFY)).andRespond(withSuccess(sucesso("LER123", -5), MediaType.APPLICATION_JSON));
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMTP fora do ar"))
                .when(email).enviarAgradecimentoPix("doador@example.org", BigDecimal.TEN);
        enviar("LER123", "token-1")
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "60"));

        // A mensagem interna do SMTP não vaza para o cliente.
        enviar("LER123", "token-2")
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void semEmailNaoHaChamadaAoBackEPixPermaneceDisponivel() throws Exception {
        // "Continuar sem e-mail" é resolvido apenas no navegador: nenhum endpoint é chamado.
        verifyNoInteractions(email);
        turnstile.verify();

        // Corpo sem e-mail ou com e-mail em branco nunca chega ao serviço nem ao provedor.
        mvc.perform(post("/pix/codigo/LER123/agradecimento").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\",\"captchaToken\":\"token\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/pix/codigo/LER123/agradecimento").contentType(MediaType.APPLICATION_JSON)
                .content("{\"captchaToken\":\"token\"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(email);
        turnstile.verify();

        // O código segue válido: quem desistiu do e-mail pode voltar a pedir depois.
        turnstile.expect(requestTo(SITEVERIFY)).andRespond(withSuccess(sucesso("LER123", -5), MediaType.APPLICATION_JSON));
        enviar("LER123", "token").andExpect(status().isNoContent());
    }

    @Test
    void codigoDesconhecidoOuComPrazoEncerradoNaoChamaOProvedor() throws Exception {
        enviar("INEXISTENTE", "token").andExpect(status().isNotFound());
        avancar(901);
        enviar("LER123", "token").andExpect(status().isGone());
        turnstile.verify();
        verifyNoInteractions(email);
    }

    private org.springframework.test.web.servlet.ResultActions enviar(String identificador, String token) throws Exception {
        return mvc.perform(post("/pix/codigo/{id}/agradecimento", identificador)
                .contentType(MediaType.APPLICATION_JSON).content(CORPO.formatted(token)));
    }

    private String sucesso(String cdata, long segundosRelativos) {
        return "{\"success\":true,\"challenge_ts\":\"" + agora.get().plusSeconds(segundosRelativos)
                + "\",\"hostname\":\"doar.example.org\",\"action\":\"agradecimento_pix\",\"cdata\":\"" + cdata + "\"}";
    }

    private void avancar(long segundos) {
        agora.updateAndGet(instante -> instante.plusSeconds(segundos));
    }

    private void registrar(String id) {
        OffsetDateTime geradoEm = agora.get().atOffset(ZoneOffset.UTC);
        agradecimento.registrar(new CodigoPixResponseDTO(id, BigDecimal.TEN, "doacoes@example.org",
                "LAR ESCOLA REDENCAO", "ARARAQUARA", "payload", "imagem", geradoEm, geradoEm.plusMinutes(15)));
    }
}
