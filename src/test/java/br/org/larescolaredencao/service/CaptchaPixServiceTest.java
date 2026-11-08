package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CaptchaPixServiceTest {
    private MockRestServiceServer servidor;
    private CaptchaPixService service;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://captcha.example/siteverify");
        servidor = MockRestServiceServer.bindTo(builder).build();
        service = new CaptchaPixService(builder.build(), "segredo", "doar.example.org",
                Clock.fixed(Instant.parse("2026-11-08T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void aceitaSomenteTokenValidoNoContextoEsperado() {
        responder("{\"success\":true,\"challenge_ts\":\"2026-11-08T11:59:00Z\","
                + "\"hostname\":\"doar.example.org\",\"action\":\"agradecimento_pix\",\"cdata\":\"LER123\"}");
        service.validar("token", "127.0.0.1", "LER123");
        servidor.verify();
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"login", "recuperacao_senha"})
    void aceitaComprovacaoDaAutenticacaoNoContextoEsperado(String acao) {
        responder("{\"success\":true,\"challenge_ts\":\"2026-11-08T11:59:00Z\","
                + "\"hostname\":\"doar.example.org\",\"action\":\"" + acao + "\",\"cdata\":\"" + acao + "\"}");
        service.validar("token", "127.0.0.1", acao, acao);
        servidor.verify();
    }

    @Test
    void naoAceitaComprovacaoDoPixNoLogin() {
        responder("{\"success\":true,\"challenge_ts\":\"2026-11-08T11:59:00Z\","
                + "\"hostname\":\"doar.example.org\",\"action\":\"agradecimento_pix\",\"cdata\":\"LER123\"}");
        assertThatThrownBy(() -> service.validar("token", "127.0.0.1", "login", "login"))
                .hasMessageContaining("400");
        servidor.verify();
    }

    @ParameterizedTest
    @MethodSource("respostasInvalidas")
    void rejeitaInvalidosExpiradosReutilizadosEForaDeContexto(String resposta) {
        responder(resposta);
        assertThatThrownBy(() -> service.validar("token", "127.0.0.1", "LER123"))
                .hasMessageContaining("400");
        servidor.verify();
    }

    static Stream<Arguments> respostasInvalidas() {
        String base = "{\"success\":true,\"challenge_ts\":\"%s\",\"hostname\":\"%s\","
                + "\"action\":\"%s\",\"cdata\":\"%s\"}";
        return Stream.of(
                Arguments.of("{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}"),
                Arguments.of("{\"success\":false,\"error-codes\":[\"timeout-or-duplicate\"]}"),
                Arguments.of(base.formatted("2026-11-08T11:55:00Z", "doar.example.org", "agradecimento_pix", "LER123")),
                Arguments.of(base.formatted("2026-11-08T12:01:00Z", "doar.example.org", "agradecimento_pix", "LER123")),
                Arguments.of(base.formatted("2026-11-08T11:59:00Z", "outro.example.org", "agradecimento_pix", "LER123")),
                Arguments.of(base.formatted("2026-11-08T11:59:00Z", "doar.example.org", "login", "LER123")),
                Arguments.of(base.formatted("2026-11-08T11:59:00Z", "doar.example.org", "agradecimento_pix", "LER456")),
                Arguments.of("{\"success\":true}"));
    }

    @Test
    void captchaAusenteNaoConsultaProvedor() {
        assertThatThrownBy(() -> service.validar("", "127.0.0.1", "LER123")).hasMessageContaining("400");
        servidor.verify();
    }

    @Test
    void falhaDoProvedorRetornaMensagemAmigavel() {
        servidor.expect(requestTo("https://captcha.example/siteverify")).andRespond(withServerError());
        assertThatThrownBy(() -> service.validar("token", "127.0.0.1", "LER123"))
                .hasMessageContaining("503").hasMessageContaining("indisponível");
        servidor.verify();
    }

    @Test
    void naoAceitaChavesOficiaisDeTesteSemOptInLocal() {
        service = new CaptchaPixService(RestClient.create(), "1x0000000000000000000000000000000AA",
                "example.com", Clock.systemUTC());
        assertThatThrownBy(() -> service.validar("token", "127.0.0.1", "LER123")).hasMessageContaining("503");
    }

    private void responder(String json) {
        servidor.expect(requestTo("https://captcha.example/siteverify"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }
}
