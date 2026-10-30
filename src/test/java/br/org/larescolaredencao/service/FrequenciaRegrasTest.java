package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.*;
import br.org.larescolaredencao.model.*;
import br.org.larescolaredencao.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FrequenciaRegrasTest {
    @Mock FrequenciaRepository frequencias;
    @Mock MatriculaRepository matriculas;
    @Mock OcorrenciaRepository ocorrencias;
    @Mock TurmaRepository turmas;
    FrequenciaService service;
    Turma turma;
    Matricula matricula;
    Membro monitor;
    final LocalDate dia = LocalDate.of(2026, 9, 1);
    final LocalDateTime criacao = LocalDateTime.of(2026, 10, 31, 15, 0);

    @BeforeEach void preparar() {
        service = new FrequenciaService(frequencias, matriculas, ocorrencias, turmas);
        Unidade unidade = new Unidade(); unidade.setId(1);
        turma = new Turma(); turma.setId(1); turma.setUnidade(unidade); turma.setHoraInicio(LocalTime.of(8, 0));
        matricula = new Matricula(); matricula.setId(10); matricula.setTurma(turma);
        matricula.setDataIngresso(LocalDateTime.of(2026, 8, 1, 0, 0));
        Usuario usuario = new Usuario(); usuario.setId(20); usuario.setNomeCompleto("Aluno");
        usuario.setMatriculas(List.of(matricula)); matricula.setUsuario(usuario);
        monitor = new Membro(); monitor.setId(8); monitor.setPapel(new Papel(3, "MONITOR", "")); monitor.setUnidades(List.of(unidade));
        lenient().when(turmas.findByIdComBloqueio(1)).thenReturn(Optional.of(turma));
        lenient().when(matriculas.findById(10)).thenReturn(Optional.of(matricula));
    }

    SalvarFrequenciaEmLoteDTO lote() {
        var item = new FrequenciaDTO(); item.setIdMatricula(10); item.setPresente(true);
        var dto = new SalvarFrequenciaEmLoteDTO(); dto.setIdTurma(1); dto.setData(dia); dto.setFrequencias(List.of(item)); return dto;
    }

    Frequencia existente(LocalDateTime dataCriacao) {
        Frequencia f = new Frequencia(); f.setId(1); f.setMatricula(matricula); f.setDataRegistro(dia);
        f.setDataCriacao(dataCriacao); f.setPresente(false); return f;
    }

    Frequencia deOutroAluno(LocalDateTime dataCriacao) {
        Matricula outra = new Matricula(); outra.setId(11); outra.setTurma(turma);
        outra.setDataIngresso(matricula.getDataIngresso());
        Usuario usuario = new Usuario(); usuario.setId(21); usuario.setNomeCompleto("Outro aluno");
        usuario.setMatriculas(List.of(outra)); outra.setUsuario(usuario);
        Frequencia frequencia = existente(dataCriacao); frequencia.setId(2); frequencia.setMatricula(outra);
        return frequencia;
    }

    void proibido(Runnable operacao) {
        assertThatThrownBy(operacao::run).isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test void todoNovoRegistroRecebeCriacaoDoServidorParaQualquerPerfil() {
        Matricula outra = deOutroAluno(criacao).getMatricula();
        when(matriculas.findById(11)).thenReturn(Optional.of(outra));
        var dto = lote();
        var item = new FrequenciaDTO(); item.setIdMatricula(11); item.setPresente(false);
        dto.setFrequencias(List.of(dto.getFrequencias().get(0), item));
        try (var tempo = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(criacao);
            for (String papel : List.of("MONITOR", "ADMINISTRADOR", "COORDENADOR")) {
                monitor.setPapel(new Papel(1, papel, ""));
                service.salvarFrequenciaEmLote(dto, monitor);
            }
        }
        verify(frequencias, times(3)).saveAllAndFlush(argThat(registros -> {
            int quantidade = 0;
            for (Frequencia registro : registros) {
                assertThat(registro.getDataCriacao()).isEqualTo(criacao);
                assertThat(registro.getDataRegistro()).isEqualTo(dia);
                quantidade++;
            }
            return quantidade == 2;
        }));
    }

    @Test void correcaoNaViradaDoMesPreservaCriacaoEPermiteQualquerMonitor() {
        Frequencia registro = existente(criacao);
        Membro autor = new Membro(); autor.setId(99); registro.setMembro(autor);
        when(frequencias.findByMatriculaTurmaIdAndDataRegistro(1, dia)).thenReturn(List.of(registro));
        when(frequencias.findByMatriculaIdAndDataRegistro(10, dia)).thenReturn(Optional.of(registro));
        try (var tempo = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(criacao.plusHours(48));
            service.atualizarFrequenciaEmLote(lote(), monitor);
            service.salvarFrequenciaEmLote(lote(), monitor);
            assertThat(registro.getDataCriacao()).isEqualTo(criacao);
            tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(criacao.plusHours(48).plusSeconds(1));
            registro.setPresente(false);
            proibido(() -> service.atualizarFrequenciaEmLote(lote(), monitor));
            proibido(() -> service.salvarFrequenciaEmLote(lote(), monitor));
            assertThat(registro.getPresente()).isFalse();
        }
        verify(frequencias, times(2)).saveAllAndFlush(any());
    }

    @Test void alunoIncluidoDepoisRecebeSuaCriacaoSemRenovarPrazoColetivo() {
        Frequencia anterior = deOutroAluno(criacao);
        when(frequencias.findByMatriculaTurmaIdAndDataRegistro(1, dia)).thenReturn(List.of(anterior));
        try (var tempo = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(criacao.plusHours(47));
            service.salvarFrequenciaEmLote(lote(), monitor);
            tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(criacao.plusHours(49));
            proibido(() -> service.salvarFrequenciaEmLote(lote(), monitor));
        }
        verify(frequencias).saveAllAndFlush(argThat(registros ->
                registros.iterator().next().getDataCriacao().equals(criacao.plusHours(47))));
        assertThat(anterior.getDataCriacao()).isEqualTo(criacao);
    }

    @Test void putIndividualUsaMenorCriacaoDaTurmaEDiaMesmoQuandoAlunoFoiIncluidoDepois() {
        Frequencia primeiro = deOutroAluno(criacao);
        Frequencia recente = existente(criacao.plusHours(47)); recente.setId(2);
        when(frequencias.findById(2)).thenReturn(Optional.of(recente));
        when(frequencias.findByMatriculaTurmaIdAndDataRegistro(1, dia)).thenReturn(List.of(recente, primeiro));
        when(frequencias.save(recente)).thenReturn(recente);
        AtualizarFrequenciaDTO dto = new AtualizarFrequenciaDTO(); dto.setPresente(true);
        try (var tempo = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(criacao.plusHours(48));
            var resposta = service.atualizarFrequencia(2, dto, monitor);
            assertThat(resposta.getDataCriacaoChamada()).isEqualTo(criacao);
            assertThat(recente.getDataCriacao()).isEqualTo(criacao.plusHours(47));
            tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(criacao.plusHours(48).plusSeconds(1));
            proibido(() -> service.atualizarFrequencia(2, dto, monitor));
        }
        verify(frequencias).save(recente);
    }

    @Test void listagemInformaMesmaPrimeiraCriacaoParaTodaATurma() {
        Frequencia primeiro = deOutroAluno(criacao);
        Frequencia recente = existente(criacao.plusHours(47));
        when(turmas.findById(1)).thenReturn(Optional.of(turma));
        when(matriculas.findHistoricoAtivasPorTurmaEData(1, dia.plusDays(1).atStartOfDay(), dia)).thenReturn(List.of(matricula, primeiro.getMatricula()));
        when(frequencias.findByMatriculaTurmaIdAndDataRegistro(1, dia)).thenReturn(List.of(recente, primeiro));
        var resposta = service.listarFrequencia(1, dia, monitor);
        assertThat(resposta).hasSize(2);
        assertThat(resposta).extracting(FrequenciaUsuarioResponseDTO::getDataCriacaoChamada).containsOnly(criacao);
        assertThat(resposta).extracting(FrequenciaUsuarioResponseDTO::getDataCriacao).containsExactly(criacao.plusHours(47), criacao);
        assertThat(resposta.get(0).getChamadaRegistrada()).isTrue();
    }

    @Test void legadoNuloMesmoComRegistroRecenteNaoGanhaNovaJanela() {
        when(frequencias.findByMatriculaTurmaIdAndDataRegistro(1, dia)).thenReturn(List.of(existente(null), existente(criacao)));
        try (var tempo = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(criacao.plusHours(1));
            proibido(() -> service.salvarFrequenciaEmLote(lote(), monitor));
        }
        verify(frequencias, never()).saveAllAndFlush(any());
    }

    @Test void administradorECoordenadorCorrigemLegadoSemInventarCriacao() {
        Frequencia registro = existente(null);
        when(frequencias.findByMatriculaTurmaIdAndDataRegistro(1, dia)).thenReturn(List.of(registro));
        when(frequencias.findByMatriculaIdAndDataRegistro(10, dia)).thenReturn(Optional.of(registro));
        for (String papel : List.of("ADMINISTRADOR", "COORDENADOR")) {
            monitor.setPapel(new Papel(1, papel, ""));
            service.salvarFrequenciaEmLote(lote(), monitor);
        }
        assertThat(registro.getDataCriacao()).isNull();
        verify(frequencias, times(2)).saveAllAndFlush(any());
    }

    @Test void acessoAOutraUnidadeNaoGravaPresencas() {
        monitor.setUnidades(List.of());
        proibido(() -> service.salvarFrequenciaEmLote(lote(), monitor));
        proibido(() -> service.atualizarFrequenciaEmLote(lote(), monitor));
        verifyNoInteractions(frequencias);
    }

    @Test void todosOsPerfisRespeitamInicioEFuturoEmLoteEIndividual() {
        LocalDateTime inicio = criacao.withHour(8);
        var dto = lote(); dto.setData(inicio.toLocalDate());
        AtualizarFrequenciaDTO edicao = new AtualizarFrequenciaDTO(); edicao.setPresente(true);
        Frequencia registro = existente(inicio.minusMinutes(1)); registro.setDataRegistro(dto.getData());
        lenient().when(frequencias.findByMatriculaIdAndDataRegistro(10, dto.getData())).thenReturn(Optional.of(registro));
        lenient().when(frequencias.findById(1)).thenReturn(Optional.of(registro));
        lenient().when(frequencias.save(registro)).thenReturn(registro);
        try (var tempo = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            for (String papel : List.of("MONITOR", "ADMINISTRADOR", "COORDENADOR")) {
                monitor.setPapel(new Papel(1, papel, ""));
                when(frequencias.findByMatriculaTurmaIdAndDataRegistro(1, dto.getData())).thenReturn(List.of());
                tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(inicio.minusSeconds(1));
                proibido(() -> service.salvarFrequenciaEmLote(dto, monitor));
                when(frequencias.findByMatriculaTurmaIdAndDataRegistro(1, dto.getData())).thenReturn(List.of(registro));
                proibido(() -> service.atualizarFrequenciaEmLote(dto, monitor));
                proibido(() -> service.atualizarFrequencia(1, edicao, monitor));
                tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(inicio.minusDays(1).withHour(15));
                proibido(() -> service.salvarFrequenciaEmLote(dto, monitor));
                proibido(() -> service.atualizarFrequenciaEmLote(dto, monitor));
                proibido(() -> service.atualizarFrequencia(1, edicao, monitor));
                for (LocalDateTime agora : List.of(inicio, inicio.plusHours(1))) {
                    tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(agora);
                    service.salvarFrequenciaEmLote(dto, monitor);
                    service.atualizarFrequenciaEmLote(dto, monitor);
                    service.atualizarFrequencia(1, edicao, monitor);
                }
            }
        }
        verify(frequencias, times(12)).saveAllAndFlush(any());
        verify(frequencias, times(6)).save(registro);
    }

    @Test void novoCadastroForaDaJanelaOuAntesDaAulaRetorna403() {
        try (var tempo = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(criacao.plusDays(1));
            proibido(() -> service.salvarFrequenciaEmLote(lote(), monitor));
            var hoje = lote(); hoje.setData(criacao.toLocalDate());
            tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(criacao.withHour(7));
            proibido(() -> service.salvarFrequenciaEmLote(hoje, monitor));
        }
        verify(frequencias, never()).saveAllAndFlush(any());
    }
}
