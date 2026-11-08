package br.org.larescolaredencao.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
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

import br.org.larescolaredencao.dto.CodigoPixResponseDTO;

@Service
public class AgradecimentoPixService {
    private static final int LIMITE_CODIGOS_EM_ABERTO = 5000;
    private static final int LIMITE_CHAVES_PROTECAO = 20000;
    private final EmailService emailService;
    private final CaptchaPixService captchaService;
    private final Clock relogio;
    private final Map<String, RegistroAgradecimento> registros = new HashMap<>();
    private final Map<String, ArrayDeque<Instant>> tentativasIp = new HashMap<>();
    private final Map<String, ArrayDeque<Instant>> tentativasEmail = new HashMap<>();
    private final Map<String, Instant> tokensUtilizados = new HashMap<>();

    @Autowired
    public AgradecimentoPixService(EmailService emailService, CaptchaPixService captchaService) {
        this(emailService, captchaService, Clock.systemUTC());
    }

    AgradecimentoPixService(EmailService emailService, CaptchaPixService captchaService, Clock relogio) {
        this.emailService = emailService;
        this.captchaService = captchaService;
        this.relogio = relogio;
    }

    public synchronized void registrar(CodigoPixResponseDTO codigo) {
        limparExpirados(relogio.instant());
        if (registros.size() >= LIMITE_CODIGOS_EM_ABERTO) {
            throw bloqueio("Muitos códigos em aberto. Tente novamente em alguns minutos.", 60);
        }
        registros.putIfAbsent(codigo.identificador(), new RegistroAgradecimento(codigo));
    }

    public void agradecer(String identificador, String email, String token, String ip) {
        String destinatario = email.trim().toLowerCase(Locale.ROOT);
        RegistroAgradecimento registro = reservar(identificador, destinatario, token, ip);
        try {
            captchaService.validar(token, ip, identificador);
            emailService.enviarAgradecimentoPix(destinatario, registro.codigo.valor());
            synchronized (this) {
                registro.enviado = true;
            }
        } catch (ResponseStatusException e) {
            throw new BloqueioAgradecimentoException(
                    e.getStatusCode().is5xxServerError() ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_REQUEST,
                    e.getReason(), segundosRestantes(registro.tentativaEm.plusSeconds(60), relogio.instant()));
        } catch (RuntimeException e) {
            throw new BloqueioAgradecimentoException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível enviar o agradecimento. Aguarde e tente novamente ou conclua sem e-mail.",
                    segundosRestantes(registro.tentativaEm.plusSeconds(60), relogio.instant()));
        } finally {
            synchronized (this) {
                registro.emAndamento = false;
            }
        }
    }

    // A reserva é única para código, token, IP e destinatário; rede/SMTP ficam fora do lock.
    private synchronized RegistroAgradecimento reservar(String identificador, String email, String token, String ip) {
        Instant agora = relogio.instant();
        limparExpirados(agora);
        RegistroAgradecimento registro = registros.get(identificador);
        if (registro == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Código indisponível. Gere um novo Pix.");
        }
        if (!registro.codigo.expiraEm().toInstant().isAfter(agora)) {
            throw new ResponseStatusException(HttpStatus.GONE, "O prazo desta sessão terminou. Gere um novo Pix.");
        }
        if (registro.enviado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O agradecimento deste Pix já foi enviado.");
        }
        if (registro.emAndamento) {
            throw bloqueio("O agradecimento deste Pix está em andamento. Aguarde.", 60);
        }
        if (token == null || token.isBlank() || token.length() > 2048) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faça uma nova verificação de segurança.");
        }
        String hashToken = hash(token);
        if (tokensUtilizados.containsKey(hashToken)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Esta verificação já foi utilizada. Faça uma nova verificação.");
        }
        String hashEmail = hash(email);
        String hashIp = hash(ip);
        long espera = registro.tentativaEm == null ? 0
                : segundosRestantes(registro.tentativaEm.plusSeconds(60), agora);
        espera = Math.max(espera, esperaLimite(tentativasEmail.get(hashEmail), agora, 60, 1));
        espera = Math.max(espera, esperaLimite(tentativasEmail.get(hashEmail), agora, 3600, 5));
        espera = Math.max(espera, esperaLimite(tentativasIp.get(hashIp), agora, 60, 10));
        espera = Math.max(espera, esperaLimite(tentativasIp.get(hashIp), agora, 3600, 100));
        if (espera > 0) {
            throw bloqueio("Limite de solicitações atingido. Aguarde o tempo indicado para tentar novamente.", espera);
        }
        if (tokensUtilizados.size() >= LIMITE_CHAVES_PROTECAO
                || (!tentativasEmail.containsKey(hashEmail) && tentativasEmail.size() >= LIMITE_CHAVES_PROTECAO)
                || (!tentativasIp.containsKey(hashIp) && tentativasIp.size() >= LIMITE_CHAVES_PROTECAO)) {
            throw bloqueio("Muitas solicitações no momento. Aguarde e tente novamente.", 60);
        }
        tentativasEmail.computeIfAbsent(hashEmail, chave -> new ArrayDeque<>()).addLast(agora);
        tentativasIp.computeIfAbsent(hashIp, chave -> new ArrayDeque<>()).addLast(agora);
        tokensUtilizados.put(hashToken, agora.plusSeconds(330));
        registro.tentativaEm = agora;
        registro.emAndamento = true;
        return registro;
    }

    private long esperaLimite(ArrayDeque<Instant> tentativas, Instant agora, long janela, int limite) {
        if (tentativas == null) return 0;
        var recentes = tentativas.stream().filter(instante -> instante.plusSeconds(janela).isAfter(agora)).toList();
        return recentes.size() < limite ? 0
                : segundosRestantes(recentes.get(recentes.size() - limite).plusSeconds(janela), agora);
    }

    private long segundosRestantes(Instant fim, Instant agora) {
        return Math.max(0, (java.time.Duration.between(agora, fim).toMillis() + 999) / 1000);
    }

    private void limparExpirados(Instant agora) {
        registros.values().removeIf(registro -> !registro.emAndamento
                && registro.codigo.expiraEm().toInstant().plusSeconds(60).isBefore(agora));
        tokensUtilizados.values().removeIf(fim -> !fim.isAfter(agora));
        limparTentativas(tentativasIp, agora);
        limparTentativas(tentativasEmail, agora);
    }

    private void limparTentativas(Map<String, ArrayDeque<Instant>> mapa, Instant agora) {
        mapa.values().removeIf(tentativas -> {
            while (!tentativas.isEmpty() && !tentativas.peekFirst().plusSeconds(3600).isAfter(agora)) {
                tentativas.removeFirst();
            }
            return tentativas.isEmpty();
        });
    }

    private BloqueioAgradecimentoException bloqueio(String mensagem, long segundos) {
        return new BloqueioAgradecimentoException(HttpStatus.TOO_MANY_REQUESTS, mensagem, segundos);
    }

    private String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class RegistroAgradecimento {
        private final CodigoPixResponseDTO codigo;
        private boolean enviado;
        private boolean emAndamento;
        private Instant tentativaEm;

        private RegistroAgradecimento(CodigoPixResponseDTO codigo) {
            this.codigo = codigo;
        }
    }
}
