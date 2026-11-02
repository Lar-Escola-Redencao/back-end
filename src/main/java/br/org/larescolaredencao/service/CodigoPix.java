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
    private static final BigDecimal VALOR_MINIMO = new BigDecimal("0.01");
    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("999999.99");
    private static final int TAMANHO_BASE_CPF = 9;
    private static final int TAMANHO_BASE_CNPJ = 12;
    private static final int POLINOMIO_CRC16 = 0x1021;
    private static final int MASCARA_16_BITS = 0xFFFF;

    private CodigoPix() {
    }

    static String normalizarChave(String entrada) {
        String chave = entrada == null ? "" : entrada.trim();
        if (chave.contains("@")) {
            return normalizarEmail(chave);
        }

        String chaveMinuscula = chave.toLowerCase(Locale.ROOT);
        if (ALEATORIA.matcher(chaveMinuscula).matches()) {
            return chaveMinuscula;
        }

        String chaveSemMascara = chave.replaceAll("[.()/\\s-]", "").toUpperCase(Locale.ROOT);
        boolean telefoneValido = chaveSemMascara.matches("\\+[1-9][0-9]{1,14}");
        if (telefoneValido || cpfValido(chaveSemMascara) || cnpjValido(chaveSemMascara)) {
            return chaveSemMascara;
        }
        throw chaveInvalida();
    }

    private static String normalizarEmail(String chave) {
        String email = chave.toLowerCase(Locale.ROOT);
        if (email.length() > 77 || !EMAIL.matcher(email).matches()) {
            throw chaveInvalida();
        }
        return email;
    }

    private static ResponseStatusException chaveInvalida() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Informe uma chave Pix válida: CPF, CNPJ, e-mail, telefone com código do país (+55) ou chave aleatória.");
    }

    private static boolean cpfValido(String chave) {
        return chave.matches("[0-9]{11}") && digitosVerificadoresValidos(chave, TAMANHO_BASE_CPF);
    }

    private static boolean cnpjValido(String chave) {
        return chave.matches("[A-Z0-9]{12}[0-9]{2}") && digitosVerificadoresValidos(chave, TAMANHO_BASE_CNPJ);
    }

    private static boolean digitosVerificadoresValidos(String documento, int tamanhoBase) {
        if (documento.chars().distinct().count() == 1) {
            return false;
        }
        for (int indiceDigito = tamanhoBase; indiceDigito < documento.length(); indiceDigito++) {
            int digitoEsperado = calcularDigitoVerificador(documento, indiceDigito, tamanhoBase);
            int digitoInformado = documento.charAt(indiceDigito) - '0';
            if (digitoInformado != digitoEsperado) {
                return false;
            }
        }
        return true;
    }

    private static int calcularDigitoVerificador(String documento, int indiceDigito, int tamanhoBase) {
        int soma = 0;
        for (int indice = 0; indice < indiceDigito; indice++) {
            int peso = tamanhoBase == TAMANHO_BASE_CNPJ
                    ? ((indiceDigito - 1 - indice) % 8) + 2
                    : indiceDigito + 1 - indice;
            soma += (documento.charAt(indice) - '0') * peso;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
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
        String valorFormatado = formatarValor(valor);
        validarIdentificador(identificador);

        String dadosRecebedor = campo("00", "br.gov.bcb.pix") + campo("01", normalizarChave(chave));
        String dadosAdicionais = campo("05", identificador);
        String payload = campo("00", "01")
                + campo("26", dadosRecebedor)
                + campo("52", "0000")
                + campo("53", "986")
                + campo("54", valorFormatado)
                + campo("58", "BR")
                + campo("59", normalizarNome(nome, 25))
                + campo("60", normalizarNome(cidade, 15))
                + campo("62", dadosAdicionais)
                + "6304";
        return payload + crc16(payload);
    }

    private static String formatarValor(BigDecimal valor) {
        if (valor == null || valor.compareTo(VALOR_MINIMO) < 0 || valor.compareTo(VALOR_MAXIMO) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um valor entre R$ 0,01 e R$ 999.999,99.");
        }
        try {
            return valor.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
        } catch (ArithmeticException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um valor com até duas casas decimais.");
        }
    }

    private static void validarIdentificador(String identificador) {
        if (!identificador.matches("[A-Za-z0-9]{1,25}|\\*\\*\\*")) {
            throw new IllegalArgumentException("Identificador Pix inválido.");
        }
    }

    private static String campo(String id, String valor) {
        int tamanho = valor.getBytes(StandardCharsets.UTF_8).length;
        if (tamanho > 99) {
            throw new IllegalArgumentException("Campo Pix excede o tamanho permitido.");
        }
        return id + String.format(Locale.ROOT, "%02d", tamanho) + valor;
    }

    static String crc16(String payload) {
        int crc = MASCARA_16_BITS;
        for (byte valor : payload.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (valor & 0xFF) << 8;
            for (int bit = 0; bit < 8; bit++) {
                boolean bitMaisSignificativoAtivo = (crc & 0x8000) != 0;
                crc <<= 1;
                if (bitMaisSignificativoAtivo) {
                    crc ^= POLINOMIO_CRC16;
                }
                crc &= MASCARA_16_BITS;
            }
        }
        return String.format(Locale.ROOT, "%04X", crc);
    }
}
