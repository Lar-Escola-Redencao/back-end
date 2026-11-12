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

    private static final int LIMITE_CODIGOS_EM_ABERTO = 5000;
    /** Mesmo formato aceito no campo de identificação do BR Code (ver CodigoPix). */
    private static final java.util.regex.Pattern FORMATO_IDENTIFICADOR = java.util.regex.Pattern.compile("[A-Za-z0-9]{1,25}");

    private final EmailService emailService;
    private final Map<String, RegistroAgradecimento> registros = new HashMap<>();

    public AgradecimentoPixService(EmailService emailService) {
        this.emailService = emailService;
    }

    public synchronized void registrar(CodigoPixResponseDTO codigo) {
        removerRegistrosExpirados();
        if (registros.size() >= LIMITE_CODIGOS_EM_ABERTO) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitos códigos em aberto. Tente novamente em alguns minutos.");
        }
        registros.put(codigo.identificador(), new RegistroAgradecimento(codigo));
    }

    public void agradecer(String identificador, String email) {
        RegistroAgradecimento registro = buscarRegistro(identificador);
        synchronized (registro) {
            validarEnvioDisponivel(registro);
            emailService.enviarAgradecimentoPix(email.trim(), registro.codigo.valor());
            registro.enviado = true;
        }
    }

    private void removerRegistrosExpirados() {
        OffsetDateTime agora = OffsetDateTime.now();
        registros.values().removeIf(registro -> !registro.codigo.expiraEm().isAfter(agora));
    }

    private synchronized RegistroAgradecimento buscarRegistro(String identificador) {
        RegistroAgradecimento registro = identificador != null && FORMATO_IDENTIFICADOR.matcher(identificador).matches()
                ? registros.get(identificador)
                : null;
        if (registro == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Código indisponível. Gere um novo Pix.");
        }
        return registro;
    }

    private void validarEnvioDisponivel(RegistroAgradecimento registro) {
        if (!registro.codigo.expiraEm().isAfter(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "O prazo desta sessão terminou. Gere um novo Pix.");
        }
        if (registro.enviado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O agradecimento deste Pix já foi enviado.");
        }
    }

    private static class RegistroAgradecimento {
        private final CodigoPixResponseDTO codigo;
        private boolean enviado;

        private RegistroAgradecimento(CodigoPixResponseDTO codigo) {
            this.codigo = codigo;
        }
    }
}
