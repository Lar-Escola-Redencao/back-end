package br.org.larescolaredencao.service;

import br.org.larescolaredencao.model.*;
import br.org.larescolaredencao.model.enums.Periodo;
import br.org.larescolaredencao.model.enums.StatusMatricula;
import br.org.larescolaredencao.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FrequenciaAcompanhamentoServiceTest {
    final UsuarioService usuarios = mock(UsuarioService.class);
    final MatriculaRepository matriculas = mock(MatriculaRepository.class);
    final FrequenciaRepository frequencias = mock(FrequenciaRepository.class);
    final FrequenciaAcompanhamentoService service = new FrequenciaAcompanhamentoService(usuarios, matriculas,
            frequencias, Clock.fixed(Instant.parse("2026-01-01T01:00:00Z"), ZoneId.of("America/Sao_Paulo")));

    Membro membro(String perfil) {
        var m = new Membro(); var papel = new Papel(); papel.setNomePapel(perfil); m.setPapel(papel);
        var unidade = new Unidade(); unidade.setId(3); m.setUnidades(List.of(unidade)); return m;
    }
    Matricula matricula(int id) {
        var unidade = new Unidade(); unidade.setId(3); unidade.setNome("Sede São José");
        var turma = new Turma(); turma.setId(2); turma.setUnidade(unidade); turma.setPeriodo(Periodo.MANHA);
        turma.setHoraInicio(LocalTime.of(7, 0)); turma.setHoraFim(LocalTime.NOON);
        var m = new Matricula(); m.setId(id); m.setTurma(turma); m.setStatus(StatusMatricula.ATIVO);
        m.setDataIngresso(LocalDateTime.of(2025, 1, 1, 0, 0)); return m;
    }
    FrequenciaRepository.ResumoMensal mes(int ano, int mes, long presentes, long ausentes) {
        var linha = mock(FrequenciaRepository.ResumoMensal.class);
        when(linha.getAno()).thenReturn(ano); when(linha.getMes()).thenReturn(mes);
        when(linha.getPresencas()).thenReturn(presentes); when(linha.getFaltas()).thenReturn(ausentes);
        return linha;
    }

    @Test void usaAnoAtualEmBrasiliaEApenasMatriculasAcessiveisComTotaisConsistentes() {
        var membro = membro("MONITOR");
        when(matriculas.listarParaAcompanhamento(10, false, List.of(3))).thenReturn(List.of(matricula(7), matricula(8)));
        var meses = List.of(mes(2025, 2, 2, 1), mes(2025, 12, 3, 0));
        when(frequencias.resumirPorMatriculasEPeriodo(List.of(7, 8), LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)))
                .thenReturn(meses);
        var r = service.consultar(10, null, null, null, membro);
        assertThat(r.dataInicio()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(r.dataFim()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(r.presencas()).isEqualTo(5); assertThat(r.faltas()).isEqualTo(1);
        assertThat(r.totalRegistros()).isEqualTo(6);
        assertThat(r.meses()).extracting(m -> m.presencas() + m.faltas()).containsExactly(3L, 3L);
        verify(usuarios).buscarUsuarioPorId(10, membro);
    }

    @Test void filtraUmaMatriculaEEnviaDataFinalInclusivaSemCriarFaltas() {
        var admin = membro("ADMINISTRADOR"); var inicio = LocalDate.of(2026, 1, 31);
        when(matriculas.listarParaAcompanhamento(10, true, List.of(-1))).thenReturn(List.of(matricula(7), matricula(8)));
        var meses = List.of(mes(2026, 1, 1, 0));
        when(frequencias.resumirPorMatriculasEPeriodo(List.of(8), inicio, inicio)).thenReturn(meses);
        var r = service.consultar(10, 8, inicio, inicio, admin);
        assertThat(r.totalRegistros()).isEqualTo(1); assertThat(r.faltas()).isZero();
        verify(frequencias).resumirPorMatriculasEPeriodo(List.of(8), inicio, inicio);
    }

    @Test void recusaMatriculaDeOutroUsuarioOuForaDasUnidadesAcessiveis() {
        var coordenador = membro("COORDENADOR");
        when(matriculas.listarParaAcompanhamento(10, false, List.of(3))).thenReturn(List.of(matricula(7)));
        assertThatThrownBy(() -> service.consultar(10, 99, null, null, coordenador))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        verifyNoInteractions(frequencias);
        assertThat(service.matriculas(10, coordenador)).extracting(m -> m.getId()).containsExactly(7);
    }

    @Test void semVinculosAcessiveisRetornaZeroSemInventarFaltas() {
        var membro = membro("MONITOR"); membro.setUnidades(List.of());
        var r = service.consultar(10, null, null, null, membro);
        assertThat(r.totalRegistros()).isZero(); assertThat(r.faltas()).isZero(); assertThat(r.meses()).isEmpty();
        verifyNoInteractions(frequencias, matriculas);
    }

    @Test void rejeitaIntervaloIncompletoOuInvertido() {
        var dia = LocalDate.of(2026, 1, 1);
        for (LocalDate[] par : List.of(new LocalDate[]{dia, null}, new LocalDate[]{null, dia}, new LocalDate[]{dia.plusDays(1), dia})) {
            assertThatThrownBy(() -> service.consultar(10, null, par[0], par[1], membro("ADMINISTRADOR")))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        }
        verifyNoInteractions(frequencias, matriculas);
    }

    @Test void acessoNegadoAoUsuarioImpedeConsultas() {
        var membro = membro("MONITOR");
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN))
                .when(usuarios).buscarUsuarioPorId(10, membro);
        assertThatThrownBy(() -> service.consultar(10, null, null, null, membro)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(frequencias, matriculas);
    }
}
