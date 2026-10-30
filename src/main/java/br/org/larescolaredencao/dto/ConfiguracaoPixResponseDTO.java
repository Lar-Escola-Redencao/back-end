package br.org.larescolaredencao.dto;

import java.math.BigDecimal;
import java.util.List;

public record ConfiguracaoPixResponseDTO(String chave, String nomeRecebedor, String cidadeRecebedor,
        List<BigDecimal> valoresSugeridos) {
}
