package br.org.larescolaredencao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.org.larescolaredencao.repository.MatriculaRepository;

class SobrePublicoServiceTest {
    @Test
    void retornaContagemArredondadaParaBaixoDosMeninosComUltimaMatriculaAtiva() {
        var repository = mock(MatriculaRepository.class);
        when(repository.contarUsuariosComUltimaMatriculaAtiva()).thenReturn(97L);

        var resultado = new SobrePublicoService(repository).obterIndicadores();

        assertEquals(90L, resultado.getTotalMeninos());
        assertEquals(LocalDate.of(1978, 8, 29), resultado.getDataFundacao());
        verify(repository).contarUsuariosComUltimaMatriculaAtiva();
    }

    @Test
    void retornaZeroQuandoNaoHaMeninosComMatriculaAtiva() {
        var repository = mock(MatriculaRepository.class);
        when(repository.contarUsuariosComUltimaMatriculaAtiva()).thenReturn(0L);

        assertEquals(0L, new SobrePublicoService(repository).obterIndicadores().getTotalMeninos());
        verify(repository).contarUsuariosComUltimaMatriculaAtiva();
    }
}
