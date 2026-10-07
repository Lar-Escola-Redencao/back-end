package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TermoBuscaTest {

    @Test
    void deveEnveloparTermoEmMinusculas() {
        assertThat(TermoBusca.like("  Joana ")).isEqualTo("%joana%");
    }

    @Test
    void deveRetornarNuloQuandoTermoVazio() {
        assertThat(TermoBusca.like(null)).isNull();
        assertThat(TermoBusca.like("   ")).isNull();
    }

    @Test
    void deveEscaparCuringasDigitados() {
        assertThat(TermoBusca.like("100%_!")).isEqualTo("%100!%!_!!%");
    }

    @Test
    void deveExtrairDigitosApenasDeTermoNumerico() {
        assertThat(TermoBusca.digitosLike("123.456.789-00")).isEqualTo("%12345678900%");
        assertThat(TermoBusca.digitosLike("(11) 4000-1234")).isEqualTo("%1140001234%");
        assertThat(TermoBusca.digitosLike("Rua 7")).isNull();
        assertThat(TermoBusca.digitosLike("---")).isNull();
    }
}
