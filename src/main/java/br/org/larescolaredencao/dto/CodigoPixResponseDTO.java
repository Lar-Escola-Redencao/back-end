package br.org.larescolaredencao.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CodigoPixResponseDTO(String identificador, BigDecimal valor, String chave,
        String nomeRecebedor, String cidadeRecebedor, String copiaECola, String qrCode,
        OffsetDateTime geradoEm, OffsetDateTime expiraEm) {
}
