package br.org.larescolaredencao.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CaptchaPixService {
    private final RestClient cliente;
    private final String chaveSecreta;
    private final Set<String> hostnames;
    private final Clock relogio;
    private static final Set<String> CHAVES_TESTE = Set.of(
            "1x0000000000000000000000000000000AA", "2x0000000000000000000000000000000AA",
            "3x0000000000000000000000000000000AA");

    @Autowired
    public CaptchaPixService(@Value("${app.pix.captcha.secret-key:}") String chaveSecreta,
            @Value("${app.pix.captcha.hostnames:}") String hostnames) {
        this(criarCliente(), chaveSecreta, hostnames, Clock.systemUTC());
    }

    CaptchaPixService(RestClient cliente, String chaveSecreta, String hostnames, Clock relogio) {
        this.cliente = cliente;
        this.chaveSecreta = chaveSecreta;
        this.hostnames = Arrays.stream(hostnames.split(",")).map(String::trim)
                .filter(valor -> !valor.isEmpty()).collect(Collectors.toUnmodifiableSet());
        this.relogio = relogio;
    }

    private static RestClient criarCliente() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().requestFactory(factory)
                .baseUrl("https://challenges.cloudflare.com/turnstile/v0/siteverify").build();
    }

    public void validar(String token, String ip, String identificador) {
        validar(token, ip, "agradecimento_pix", identificador);
    }

    public void validar(String token, String ip, String acao, String contexto) {
        if (token == null || token.isBlank() || token.length() > 2048) {
            throw invalido();
        }
        if (chaveSecreta.isBlank() || hostnames.isEmpty() || CHAVES_TESTE.contains(chaveSecreta)) {
            throw indisponivel();
        }
        LinkedMultiValueMap<String, String> dados = new LinkedMultiValueMap<>();
        dados.add("secret", chaveSecreta);
        dados.add("response", token);
        dados.add("remoteip", ip);
        Map<?, ?> resposta;
        try {
            resposta = cliente.post().contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(dados).retrieve().body(Map.class);
        } catch (RestClientException e) {
            throw indisponivel();
        }
        if (resposta == null) throw indisponivel();
        if (!Boolean.TRUE.equals(resposta.get("success"))) {
            Object erros = resposta.get("error-codes");
            if (erros instanceof java.util.List<?> codigos && codigos.stream().anyMatch(codigo ->
                    Set.of("internal-error", "missing-input-secret", "invalid-input-secret").contains(codigo))) {
                throw indisponivel();
            }
            throw invalido();
        }
        try {
            Instant desafio = Instant.parse((String) resposta.get("challenge_ts"));
            Instant agora = relogio.instant();
            if (!hostnames.contains(resposta.get("hostname"))
                    || !acao.equals(resposta.get("action"))
                    || !contexto.equals(resposta.get("cdata"))
                    || desafio.isAfter(agora.plusSeconds(30))
                    || !desafio.plusSeconds(300).isAfter(agora)) {
                throw invalido();
            }
        } catch (java.time.format.DateTimeParseException | ClassCastException | NullPointerException e) {
            throw invalido();
        }
    }

    private ResponseStatusException invalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A verificação expirou ou não é válida. Faça uma nova verificação e tente novamente.");
    }

    private ResponseStatusException indisponivel() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "A verificação está indisponível. Aguarde e tente novamente ou conclua sem e-mail.");
    }
}
