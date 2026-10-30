package br.org.larescolaredencao.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Codificação BR Code do Pix estático, incluindo o CRC16/CCITT-FALSE. */
final class CodigoPix {

    private static final Pattern EMAIL = Pattern.compile("[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+");
    private static final Pattern ALEATORIA = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private CodigoPix() {
    }

    static String normalizarChave(String entrada) {
        String chave = entrada == null ? "" : entrada.trim();
        if (chave.contains("@")) {
            chave = chave.toLowerCase(Locale.ROOT);
            if (chave.length() <= 77 && EMAIL.matcher(chave).matches()) return chave;
        } else if (ALEATORIA.matcher(chave.toLowerCase(Locale.ROOT)).matches()) {
            return chave.toLowerCase(Locale.ROOT);
        } else {
            chave = chave.replaceAll("[.()/\\s-]", "").toUpperCase(Locale.ROOT);
            if (chave.matches("\\+[1-9][0-9]{1,14}")) return chave;
            if (chave.matches("[0-9]{11}") && documentoValido(chave, false)) return chave;
            if (chave.matches("[A-Z0-9]{12}[0-9]{2}") && documentoValido(chave, true)) return chave;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Informe uma chave Pix válida: CPF, CNPJ, e-mail, telefone com código do país (+55) ou chave aleatória.");
    }

    private static boolean documentoValido(String documento, boolean cnpj) {
        if (documento.chars().distinct().count() == 1) return false;
        int tamanho = cnpj ? 12 : 9;
        for (int etapa = 0; etapa < 2; etapa++) {
            int soma = 0;
            for (int i = 0; i < tamanho + etapa; i++) {
                int peso = cnpj ? ((tamanho + etapa - 1 - i) % 8) + 2 : tamanho + etapa + 1 - i;
                soma += (documento.charAt(i) - '0') * peso;
            }
            int resto = soma % 11;
            if (documento.charAt(tamanho + etapa) - '0' != (resto < 2 ? 0 : 11 - resto)) return false;
        }
        return true;
    }

    static String normalizarNome(String texto, int tamanhoMaximo) {
        String normalizado = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9 $%*+./:-]", "").trim().replaceAll("\\s+", " ");
        if (normalizado.isEmpty() || normalizado.length() > tamanhoMaximo) {
            throw new IllegalStateException("Nome ou cidade do recebedor Pix fora do tamanho permitido.");
        }
        return normalizado;
    }

    static String gerar(String chave, BigDecimal valor, String nome, String cidade, String identificador) {
        if (valor == null || valor.compareTo(new BigDecimal("0.01")) < 0
                || valor.compareTo(new BigDecimal("999999.99")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um valor entre R$ 0,01 e R$ 999.999,99.");
        }
        String valorFormatado;
        try {
            valorFormatado = valor.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
        } catch (ArithmeticException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um valor com até duas casas decimais.");
        }
        if (!identificador.matches("[A-Za-z0-9]{1,25}|\\*\\*\\*")) {
            throw new IllegalArgumentException("Identificador Pix inválido.");
        }
        String payload = campo("00", "01")
                + campo("26", campo("00", "br.gov.bcb.pix") + campo("01", normalizarChave(chave)))
                + campo("52", "0000") + campo("53", "986") + campo("54", valorFormatado)
                + campo("58", "BR") + campo("59", normalizarNome(nome, 25))
                + campo("60", normalizarNome(cidade, 15)) + campo("62", campo("05", identificador)) + "6304";
        return payload + crc16(payload);
    }

    private static String campo(String id, String valor) {
        int tamanho = valor.getBytes(StandardCharsets.UTF_8).length;
        if (tamanho > 99) throw new IllegalArgumentException("Campo Pix excede o tamanho permitido.");
        return id + String.format(Locale.ROOT, "%02d", tamanho) + valor;
    }

    static String crc16(String payload) {
        int crc = 0xFFFF;
        for (byte valor : payload.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (valor & 0xFF) << 8;
            for (int bit = 0; bit < 8; bit++) {
                crc = ((crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1) & 0xFFFF;
            }
        }
        return String.format(Locale.ROOT, "%04X", crc);
    }
}
