package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.FrequenciaDTO;
import br.org.larescolaredencao.dto.AtualizarFrequenciaDTO;
import br.org.larescolaredencao.dto.SalvarFrequenciaEmLoteDTO;
import br.org.larescolaredencao.model.*;
import br.org.larescolaredencao.model.enums.StatusMatricula;
import br.org.larescolaredencao.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FrequenciaServiceTest {
    @Mock private FrequenciaRepository frequencias;
    @Mock private MatriculaRepository matriculas;
    @Mock private OcorrenciaRepository ocorrencias;
    @Mock private TurmaRepository turmas;
    private FrequenciaService service;
    private Turma turma;
    private Matricula matricula;
    private Membro membro;
    private LocalDate data;

    @BeforeEach
    void preparar() {
        service = new FrequenciaService(frequencias, matriculas, ocorrencias, turmas);
        Unidade unidade = new Unidade();
        unidade.setId(1);
        turma = new Turma();
        turma.setId(1);
        turma.setUnidade(unidade);
        turma.setHoraInicio(LocalTime.of(7, 0));
        matricula = new Matricula();
        matricula.setId(10);
        matricula.setTurma(turma);
        Usuario usuario = new Usuario();
        usuario.setId(20);
        usuario.setNomeCompleto("Aluno");
        matricula.setUsuario(usuario);
        usuario.setMatriculas(List.of(matricula));
        matricula.setDataIngresso(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusMonths(2));
        Papel papel = new Papel();
        papel.setNomePapel("MONITOR");
        membro = new Membro();
        membro.setPapel(papel);
        membro.setUnidades(List.of(unidade));
        data = LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalDate().minusDays(3);
    }

    private SalvarFrequenciaEmLoteDTO lote() {
        FrequenciaDTO item = new FrequenciaDTO();
        item.setIdMatricula(10);
        item.setPresente(true);
        SalvarFrequenciaEmLoteDTO dto = new SalvarFrequenciaEmLoteDTO();
        dto.setIdTurma(1);
        dto.setData(data);
        dto.setFrequencias(List.of(item));
        return dto;
    }

    @Test
    void postNaoDeveContornarLimiteDeEdicaoDe48Horas() {
        when(turmas.findById(1)).thenReturn(Optional.of(turma));
        when(matriculas.findById(10)).thenReturn(Optional.of(matricula));
        when(frequencias.findByMatriculaIdAndDataRegistro(10, data)).thenReturn(Optional.of(new Frequencia()));
        assertThatThrownBy(() -> service.salvarFrequenciaEmLote(lote(), membro))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(frequencias, never()).saveAllAndFlush(any());
    }

    @Test
    void deveRejeitarMatriculaDeOutraTurmaAntesDeSalvar() {
        Turma outra = new Turma();
        outra.setId(2);
        outra.setUnidade(turma.getUnidade());
        matricula.setTurma(outra);
        when(turmas.findById(1)).thenReturn(Optional.of(turma));
        when(matriculas.findById(10)).thenReturn(Optional.of(matricula));
        assertThatThrownBy(() -> service.salvarFrequenciaEmLote(lote(), membro))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(frequencias);
    }

    @Test
    void devePermitirChamadaRetroativaDeEgressoQueEstavaMatriculado() {
        matricula.setStatus(StatusMatricula.EGRESSO);
        matricula.setDataDesligamento(data.plusDays(1));
        when(turmas.findById(1)).thenReturn(Optional.of(turma));
        when(matriculas.findById(10)).thenReturn(Optional.of(matricula));
        when(frequencias.findByMatriculaIdAndDataRegistro(10, data)).thenReturn(Optional.empty());
        service.salvarFrequenciaEmLote(lote(), membro);
        verify(frequencias).saveAllAndFlush(argThat(registros -> {
            Frequencia registro = registros.iterator().next();
            return registro.getMatricula() == matricula && registro.getDataRegistro().equals(data)
                    && Boolean.TRUE.equals(registro.getPresente());
        }));
    }

    @Test
    void deveRejeitarLoteComMatriculaRepetidaSemSalvar() {
        when(turmas.findById(1)).thenReturn(Optional.of(turma));
        when(matriculas.findById(10)).thenReturn(Optional.of(matricula));
        when(frequencias.findByMatriculaIdAndDataRegistro(10, data)).thenReturn(Optional.empty());
        SalvarFrequenciaEmLoteDTO dto = lote();
        dto.setFrequencias(List.of(dto.getFrequencias().get(0), dto.getFrequencias().get(0)));
        assertThatThrownBy(() -> service.salvarFrequenciaEmLote(dto, membro))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("repetida");
        verify(frequencias, never()).saveAllAndFlush(any());
    }

    @Test
    void deveBloquearMonitorAntesDoInicioEPermitirCoordenador() {
        data = LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalDate().plusDays(1);
        when(turmas.findById(1)).thenReturn(Optional.of(turma));
        assertThatThrownBy(() -> service.salvarFrequenciaEmLote(lote(), membro))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        membro.getPapel().setNomePapel("COORDENADOR");
        when(matriculas.findById(10)).thenReturn(Optional.of(matricula));
        when(frequencias.findByMatriculaIdAndDataRegistro(10, data)).thenReturn(Optional.empty());
        service.salvarFrequenciaEmLote(lote(), membro);
        verify(frequencias).saveAllAndFlush(any());
    }

    @Test
    void devePermitirExatamente48HorasERejeitarUmSegundoDepoisPeloPut() {
        LocalDateTime referencia = LocalDateTime.of(2026, 10, 1, 7, 0);
        LocalDateTime limite = referencia.plusHours(48);
        LocalDateTime expirado = limite.plusSeconds(1);
        matricula.setDataIngresso(referencia.minusMonths(2));
        Frequencia registro = new Frequencia();
        registro.setMatricula(matricula);
        registro.setDataRegistro(referencia.toLocalDate());
        AtualizarFrequenciaDTO dto = new AtualizarFrequenciaDTO();
        dto.setPresente(false);
        when(frequencias.findById(1)).thenReturn(Optional.of(registro));
        when(frequencias.save(registro)).thenReturn(registro);
        when(ocorrencias.findByMatriculaIdAndDataOcorrencia(10, referencia.toLocalDate())).thenReturn(List.of());
        try (MockedStatic<LocalDateTime> tempo = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(limite);
            assertThat(service.atualizarFrequencia(1, dto, membro).getPresente()).isFalse();
            tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(expirado);
            assertThatThrownBy(() -> service.atualizarFrequencia(1, dto, membro))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        }
        verify(frequencias, times(1)).save(registro);
    }

    @Test
    void devePermitirPutAntigoParaCoordenadorEAdministrador() {
        Frequencia registro = new Frequencia();
        registro.setMatricula(matricula);
        registro.setDataRegistro(data);
        AtualizarFrequenciaDTO dto = new AtualizarFrequenciaDTO();
        dto.setPresente(true);
        when(frequencias.findById(1)).thenReturn(Optional.of(registro));
        when(frequencias.save(registro)).thenReturn(registro);
        when(ocorrencias.findByMatriculaIdAndDataOcorrencia(10, data)).thenReturn(List.of());
        for (String perfil : List.of("COORDENADOR", "ADMINISTRADOR")) {
            membro.getPapel().setNomePapel(perfil);
            assertThat(service.atualizarFrequencia(1, dto, membro).getPresente()).isTrue();
        }
    }

    @Test
    void devePermitirAdministradorSemVinculoERejeitarDemaisPerfisNoGet() {
        membro.setUnidades(List.of());
        when(turmas.findById(1)).thenReturn(Optional.of(turma));
        for (String perfil : List.of("COORDENADOR", "MONITOR")) {
            membro.getPapel().setNomePapel(perfil);
            assertThatThrownBy(() -> service.listarFrequencia(1, data, membro))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        }
        membro.getPapel().setNomePapel("ADMINISTRADOR");
        when(matriculas.findHistoricoAtivasPorTurmaEData(1, data.plusDays(1).atStartOfDay(), data)).thenReturn(List.of(matricula));
        when(frequencias.findByMatriculaTurmaIdAndDataRegistro(1, data)).thenReturn(List.of());
        when(ocorrencias.findByMatriculaTurmaIdAndDataOcorrencia(1, data)).thenReturn(List.of());
        assertThat(service.listarFrequencia(1, data, membro)).hasSize(1);
    }

    @Test
    void deveInformarPermissaoDePerfilEExclusaoNoGetDoDiario() {
        Unidade outraUnidade = new Unidade();
        outraUnidade.setId(2);
        Turma outraTurma = new Turma();
        outraTurma.setId(2);
        outraTurma.setUnidade(outraUnidade);

        Matricula matriculaOutraUnidade = new Matricula();
        matriculaOutraUnidade.setId(11);
        matriculaOutraUnidade.setTurma(outraTurma);
        matriculaOutraUnidade.setUsuario(matricula.getUsuario());
        matricula.getUsuario().setMatriculas(List.of(matriculaOutraUnidade));

        when(turmas.findById(1)).thenReturn(Optional.of(turma));
        when(matriculas.findHistoricoAtivasPorTurmaEData(1, data.plusDays(1).atStartOfDay(), data)).thenReturn(List.of(matricula));
        when(frequencias.findByMatriculaTurmaIdAndDataRegistro(1, data)).thenReturn(List.of());
        when(ocorrencias.findByMatriculaTurmaIdAndDataOcorrencia(1, data)).thenReturn(List.of());

        assertThat(service.listarFrequencia(1, data, membro).get(0).getPermiteAcessoPerfil()).isFalse();

        membro.getPapel().setNomePapel("ADMINISTRADOR");
        assertThat(service.listarFrequencia(1, data, membro).get(0).getPermiteAcessoPerfil()).isTrue();

        matriculaOutraUnidade.setStatus(StatusMatricula.EXCLUIDO);
        assertThat(service.listarFrequencia(1, data, membro).get(0).getPermiteAcessoPerfil()).isFalse();
        assertThat(service.listarFrequencia(1, data, membro).get(0).getIsUsuarioExcluido()).isTrue();
    }

    @Test
    void deveValidarPeriodoHistoricoNoPostParaMatriculasEncerradas() {
        LocalDate ingresso = data.minusDays(10);
        matricula.setDataIngresso(ingresso.atTime(14, 0));
        matricula.setDataDesligamento(data);
        membro.getPapel().setNomePapel("COORDENADOR");
        when(turmas.findById(1)).thenReturn(Optional.of(turma));
        when(matriculas.findById(10)).thenReturn(Optional.of(matricula));
        when(frequencias.findByMatriculaIdAndDataRegistro(10, ingresso)).thenReturn(Optional.empty());
        when(frequencias.findByMatriculaIdAndDataRegistro(10, data)).thenReturn(Optional.empty());
        for (StatusMatricula status : List.of(StatusMatricula.EGRESSO, StatusMatricula.EXCLUIDO, StatusMatricula.INATIVO)) {
            matricula.setStatus(status);
            SalvarFrequenciaEmLoteDTO dto = lote();
            dto.setData(ingresso);
            assertThatCode(() -> service.salvarFrequenciaEmLote(dto, membro)).doesNotThrowAnyException();
            dto.setData(data);
            assertThatCode(() -> service.salvarFrequenciaEmLote(dto, membro)).doesNotThrowAnyException();
            dto.setData(ingresso.minusDays(1));
            assertThatThrownBy(() -> service.salvarFrequenciaEmLote(dto, membro))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("não estava ativa");
            dto.setData(data.plusDays(1));
            assertThatThrownBy(() -> service.salvarFrequenciaEmLote(dto, membro))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("não estava ativa");
        }
        verify(frequencias, times(6)).saveAllAndFlush(any());
    }
}
