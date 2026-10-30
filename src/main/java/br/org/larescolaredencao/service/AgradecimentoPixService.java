package br.org.larescolaredencao.service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.CodigoPixResponseDTO;

@Service
public class AgradecimentoPixService {

    private final EmailService emailService;
    private final Map<String, Registro> registros = new HashMap<>();

    public AgradecimentoPixService(EmailService emailService) {
        this.emailService = emailService;
    }

    public synchronized void registrar(CodigoPixResponseDTO codigo) {
        OffsetDateTime agora = OffsetDateTime.now();
        registros.values().removeIf(registro -> !registro.codigo.expiraEm().isAfter(agora));
        if (registros.size() >= 5000) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitos códigos em aberto. Tente novamente em alguns minutos.");
        }
        registros.put(codigo.identificador(), new Registro(codigo));
    }

    public void agradecer(String identificador, String email) {
        Registro registro;
        synchronized (this) {
            registro = registros.get(identificador);
        }
        if (registro == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Código indisponível. Gere um novo Pix.");
        }
        synchronized (registro) {
            if (!registro.codigo.expiraEm().isAfter(OffsetDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.GONE, "O prazo desta sessão terminou. Gere um novo Pix.");
            }
            if (registro.enviado) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "O agradecimento deste Pix já foi enviado.");
            }
            emailService.enviarAgradecimentoPix(email.trim(), registro.codigo.valor());
            registro.enviado = true;
        }
    }

    private static class Registro {
        private final CodigoPixResponseDTO codigo;
        private boolean enviado;

        private Registro(CodigoPixResponseDTO codigo) {
            this.codigo = codigo;
        }
    }
}
