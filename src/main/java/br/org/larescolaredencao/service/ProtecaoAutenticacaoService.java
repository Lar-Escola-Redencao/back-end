package br.org.larescolaredencao.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProtecaoAutenticacaoService {
    private static final int MAX_REGISTROS = 20000;
    private final CaptchaPixService captcha;
    private final Clock relogio;
    private final Map<String, Registro> registros = new HashMap<>();
    private final Map<String, Instant> tokens = new HashMap<>();

    @Autowired
    public ProtecaoAutenticacaoService(CaptchaPixService captcha) {
        this(captcha, Clock.systemUTC());
    }

    ProtecaoAutenticacaoService(CaptchaPixService captcha, Clock relogio) {
        this.captcha = captcha;
        this.relogio = relogio;
    }

    public Tentativa iniciarLogin(String identificador, String token, String ip) {
        Tentativa tentativa = reservar("login", normalizarIdentificador(identificador), ip, token, false);
        if (tentativa.captchaObrigatorio()) validarCaptcha(tentativa, token, ip, "login");
        return tentativa;
    }

    public Tentativa iniciarRecuperacao(String email, String token, String ip) {
        Tentativa tentativa = reservar("recuperacao_senha", email.trim().toLowerCase(Locale.ROOT), ip, token, true);
        validarCaptcha(tentativa, token, ip, "recuperacao_senha");
        return tentativa;
    }

    private void validarCaptcha(Tentativa tentativa, String token, String ip, String acao) {
        try {
            captcha.validar(token, ip, acao, acao);
        } catch (RuntimeException erro) {
            concluir(tentativa, false);
            HttpStatus status = erro instanceof ResponseStatusException resposta && resposta.getStatusCode().is4xxClientError()
                    ? HttpStatus.BAD_REQUEST : HttpStatus.SERVICE_UNAVAILABLE;
            throw new ProtecaoAutenticacaoException(status,
                    status == HttpStatus.BAD_REQUEST ? "A verificação expirou ou não é válida. Faça uma nova verificação."
                            : "A verificação está indisponível. Aguarde e tente novamente.",
                    true, "recuperacao_senha".equals(acao) ? 60 : 0);
        }
    }

    private synchronized Tentativa reservar(String fluxo, String identidade, String ip, String token, boolean recuperacao) {
        Instant agora = relogio.instant();
        limpar(agora);
        String chaveConta = fluxo + ":conta:" + hash(identidade);
        String chaveIp = fluxo + ":ip:" + hash(ip);
        Registro conta = registros.get(chaveConta);
        Registro origem = registros.get(chaveIp);
        boolean exigeCaptcha = recuperacao || exigeCaptcha(conta, origem, agora);
        long espera = Math.max(espera(conta, agora, 60, recuperacao ? 1 : 10),
                espera(origem, agora, 60, recuperacao ? 10 : 60));
        if (recuperacao) {
            espera = Math.max(espera, espera(conta, agora, 3600, 5));
            espera = Math.max(espera, espera(origem, agora, 3600, 100));
        }
        if (conta != null && conta.emAndamento > 0 && recuperacao) espera = Math.max(espera, 60);
        if (espera > 0) throw new ProtecaoAutenticacaoException(HttpStatus.TOO_MANY_REQUESTS,
                "Muitas tentativas. Aguarde o tempo indicado para tentar novamente.", exigeCaptcha, espera);
        if (exigeCaptcha && (token == null || token.isBlank() || token.length() > 2048)) {
            throw new ProtecaoAutenticacaoException(HttpStatus.BAD_REQUEST,
                    "Conclua a verificação de segurança para continuar.", true, 0);
        }
        if (exigeCaptcha && tokens.containsKey(hash(token))) {
            throw new ProtecaoAutenticacaoException(HttpStatus.BAD_REQUEST,
                    "Esta verificação já foi utilizada. Faça uma nova verificação.", true, 0);
        }
        if (registros.size() + 2 > MAX_REGISTROS || (exigeCaptcha && tokens.size() >= MAX_REGISTROS)) {
            throw new ProtecaoAutenticacaoException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas solicitações no momento. Tente novamente em instantes.", exigeCaptcha, 60);
        }
        conta = registros.computeIfAbsent(chaveConta, chave -> new Registro());
        origem = registros.computeIfAbsent(chaveIp, chave -> new Registro());
        conta.tentativas.addLast(agora);
        origem.tentativas.addLast(agora);
        conta.emAndamento++;
        origem.emAndamento++;
        if (exigeCaptcha) tokens.put(hash(token), agora.plusSeconds(330));
        return new Tentativa(chaveConta, chaveIp, exigeCaptcha);
    }

    public synchronized boolean falhouLogin(Tentativa tentativa) {
        Instant agora = relogio.instant();
        Registro conta = registros.get(tentativa.conta());
        Registro origem = registros.get(tentativa.ip());
        conta.falhas.addLast(agora);
        origem.falhas.addLast(agora);
        return falhas(conta, agora) + Math.max(0, conta.emAndamento - 1) >= 5
                || falhas(origem, agora) + Math.max(0, origem.emAndamento - 1) >= 20;
    }

    public synchronized void concluir(Tentativa tentativa, boolean sucessoLogin) {
        Registro conta = registros.get(tentativa.conta());
        Registro origem = registros.get(tentativa.ip());
        if (conta != null) {
            conta.emAndamento = Math.max(0, conta.emAndamento - 1);
            if (sucessoLogin) conta.falhas.clear();
        }
        if (origem != null) origem.emAndamento = Math.max(0, origem.emAndamento - 1);
    }

    private boolean exigeCaptcha(Registro conta, Registro ip, Instant agora) {
        return falhas(conta, agora) + (conta == null ? 0 : conta.emAndamento) >= 5
                || falhas(ip, agora) + (ip == null ? 0 : ip.emAndamento) >= 20;
    }

    private long falhas(Registro registro, Instant agora) {
        return registro == null ? 0 : registro.falhas.stream().filter(t -> t.plusSeconds(900).isAfter(agora)).count();
    }

    private long espera(Registro registro, Instant agora, int janela, int limite) {
        if (registro == null) return 0;
        var recentes = registro.tentativas.stream().filter(t -> t.plusSeconds(janela).isAfter(agora)).toList();
        return recentes.size() < limite ? 0 : Math.max(0,
                (Duration.between(agora, recentes.get(recentes.size() - limite).plusSeconds(janela)).toMillis() + 999) / 1000);
    }

    private void limpar(Instant agora) {
        tokens.values().removeIf(fim -> !fim.isAfter(agora));
        registros.values().removeIf(registro -> {
            while (!registro.tentativas.isEmpty() && !registro.tentativas.peekFirst().plusSeconds(3600).isAfter(agora)) registro.tentativas.removeFirst();
            while (!registro.falhas.isEmpty() && !registro.falhas.peekFirst().plusSeconds(900).isAfter(agora)) registro.falhas.removeFirst();
            return registro.emAndamento == 0 && registro.tentativas.isEmpty() && registro.falhas.isEmpty();
        });
    }

    private String normalizarIdentificador(String identificador) {
        String valor = identificador.trim().toLowerCase(Locale.ROOT);
        return valor.contains("@") ? valor : valor.replaceAll("\\D", "");
    }

    private String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException erro) { throw new IllegalStateException(erro); }
    }

    public record Tentativa(String conta, String ip, boolean captchaObrigatorio) {}

    private static class Registro {
        private final ArrayDeque<Instant> tentativas = new ArrayDeque<>();
        private final ArrayDeque<Instant> falhas = new ArrayDeque<>();
        private int emAndamento;
    }
}
