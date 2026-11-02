package br.org.larescolaredencao.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record GerarCodigoPixDTO(
        @NotNull(message = "Informe o valor da doação.")
        @DecimalMin(value = "0.01", message = "O valor mínimo é R$ 0,01.")
        @DecimalMax(value = "999999.99", message = "O valor máximo é R$ 999.999,99.")
        @Digits(integer = 6, fraction = 2, message = "Informe um valor com até duas casas decimais.")
        BigDecimal valor) {
}
