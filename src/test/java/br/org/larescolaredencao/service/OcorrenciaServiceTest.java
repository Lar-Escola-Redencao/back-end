package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.AtualizarOcorrenciaDTO;
import br.org.larescolaredencao.dto.CriarOcorrenciaDTO;
import br.org.larescolaredencao.model.*;
import br.org.larescolaredencao.model.enums.StatusMatricula;
import br.org.larescolaredencao.model.enums.TipoOcorrencia;
import br.org.larescolaredencao.repository.MatriculaRepository;
import br.org.larescolaredencao.repository.OcorrenciaRepository;
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
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcorrenciaServiceTest {
    @Mock private OcorrenciaRepository ocorrencias;
    @Mock private MatriculaRepository matriculas;
    private OcorrenciaService service;
    private Matricula matricula;
    private Membro membro;

    @BeforeEach
    void preparar() {
        service = new OcorrenciaService(ocorrencias, matriculas);
        Unidade unidade = new Unidade();
        unidade.setId(1);
        Turma turma = new Turma();
        turma.setUnidade(unidade);
        turma.setHoraInicio(LocalTime.of(8, 0));
        turma.setHoraFim(LocalTime.of(18, 0));
        matricula = new Matricula();
        matricula.setId(10);
        matricula.setTurma(turma);
        matricula.setDataIngresso(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusMonths(2));
        Papel papel = new Papel();
        papel.setNomePapel("MONITOR");
        membro = new Membro();
        membro.setId(7);
        membro.setPapel(papel);
        membro.setUnidades(List.of(unidade));
    }

    private CriarOcorrenciaDTO dto(LocalDate data) {
        CriarOcorrenciaDTO dto = new CriarOcorrenciaDTO();
        dto.setIdMatricula(10);
        dto.setDataOcorrencia(data.atTime(8, 0));
        dto.setDescricao("Registro de acompanhamento");
        dto.setTipoOcorrencia(TipoOcorrencia.ASSISTENCIA);
        return dto;
    }

    @Test
    void deveDefinirCriacaoPeloServidorEPreservarHorarioDoFato() {
        LocalDateTime agora = LocalDateTime.of(2026, 10, 15, 14, 30, 20);
        LocalDateTime fato = agora.minusDays(2).withHour(9).withMinute(15);
        matricula.setDataIngresso(fato.minusMonths(2));
        CriarOcorrenciaDTO dto = dto(fato.toLocalDate());
        dto.setDataOcorrencia(fato);
        when(matriculas.findById(10)).thenReturn(Optional.of(matricula));
        when(ocorrencias.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        try (MockedStatic<LocalDateTime> tempo = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(agora);
            var resposta = service.criarOcorrencia(dto, membro);
            assertThat(resposta.getDataCriacao()).isEqualTo(agora);
            assertThat(resposta.getDataOcorrencia()).isEqualTo(fato);
        }
    }

    @Test
    void deveRejeitarMudancaDoDiaNaEdicao() {
        LocalDate hoje = LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalDate();
        Ocorrencia registro = new Ocorrencia();
        registro.setMatricula(matricula);
        registro.setMembro(membro);
        registro.setDataCriacao(hoje.atStartOfDay());
        registro.setDataOcorrencia(hoje.atTime(9, 0));
        when(ocorrencias.findById(1)).thenReturn(Optional.of(registro));
        AtualizarOcorrenciaDTO dto = new AtualizarOcorrenciaDTO();
        dto.setDescricao("Correção");
        dto.setTipoOcorrencia(TipoOcorrencia.SAUDE);
        for (LocalDateTime fato : List.of(hoje.minusDays(1).atTime(9, 0), hoje.plusDays(1).atTime(9, 0))) {
            dto.setDataOcorrencia(fato);
            assertThatThrownBy(() -> service.atualizarOcorrencia(1, dto, membro))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
        verify(ocorrencias, never()).save(any());
    }

    @Test
    void deveBloquearCriacaoAntesDoMesAnteriorParaMonitorEPermitirCoordenador() {
        when(matriculas.findById(10)).thenReturn(Optional.of(matricula));
        CriarOcorrenciaDTO dto = dto(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalDate().withDayOfMonth(1).minusMonths(1).minusDays(1));
        assertThatThrownBy(() -> service.criarOcorrencia(dto, membro))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verifyNoInteractions(ocorrencias);
        membro.getPapel().setNomePapel("COORDENADOR");
        when(ocorrencias.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        assertThat(service.criarOcorrencia(dto, membro).getDataOcorrencia()).isEqualTo(dto.getDataOcorrencia());
    }

    @Test
    void devePermitirCriacaoHa7DiasMesmoParaMatriculaHojeExcluida() {
        LocalDate data = LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalDate().minusDays(7);
        matricula.setStatus(StatusMatricula.EXCLUIDO);
        matricula.setDataDesligamento(data.plusDays(1));
        when(matriculas.findById(10)).thenReturn(Optional.of(matricula));
        when(ocorrencias.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        assertThat(service.criarOcorrencia(dto(data), membro).getDataOcorrencia()).isEqualTo(data.atTime(8, 0));
    }

    @Test
    void ocorrenciaRetroativaRecemCriadaPodeSerCorrigidaSemRenovarJanela() {
        when(matriculas.findById(10)).thenReturn(Optional.of(matricula));
        final Ocorrencia[] salvo = new Ocorrencia[1];
        doAnswer(invocacao -> {
            salvo[0] = invocacao.getArgument(0);
            return salvo[0];
        }).when(ocorrencias).save(any());
        LocalDate data = LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalDate().minusDays(2);
        service.criarOcorrencia(dto(data), membro);
        LocalDateTime registro = salvo[0].getDataCriacao();
        assertThat(salvo[0].getDataOcorrencia().toLocalDate()).isEqualTo(data);
        assertThat(registro.toLocalDate()).isEqualTo(data.plusDays(2));
        when(ocorrencias.findById(1)).thenReturn(Optional.of(salvo[0]));
        AtualizarOcorrenciaDTO atualizacao = new AtualizarOcorrenciaDTO();
        atualizacao.setDescricao("Correção");
        atualizacao.setTipoOcorrencia(TipoOcorrencia.SAUDE);
        atualizacao.setDataOcorrencia(data.atTime(9, 0));
        assertThatCode(() -> service.atualizarOcorrencia(1, atualizacao, membro)).doesNotThrowAnyException();
        assertThat(salvo[0].getDataCriacao()).isEqualTo(registro);
        assertThatCode(() -> service.deletarOcorrencia(1, membro)).doesNotThrowAnyException();
    }

    @Test
    void deveUsarCriacaoDoBancoParaBloquearEdicaoEExclusaoApos24Horas() {
        Ocorrencia registro = new Ocorrencia();
        registro.setMatricula(matricula);
        registro.setMembro(membro);
        registro.setDataCriacao(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusHours(24).minusMinutes(30));
        registro.setDataOcorrencia(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
        when(ocorrencias.findById(1)).thenReturn(Optional.of(registro));
        AtualizarOcorrenciaDTO dto = new AtualizarOcorrenciaDTO();
        dto.setDescricao("Correção");
        dto.setTipoOcorrencia(TipoOcorrencia.SAUDE);
        assertThatThrownBy(() -> service.atualizarOcorrencia(1, dto, membro))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> service.deletarOcorrencia(1, membro))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(ocorrencias, never()).save(any());
        verify(ocorrencias, never()).delete(any(Ocorrencia.class));
    }

    @Test
    void devePermitirExatamente24HorasERejeitarUmSegundoDepois() {
        LocalDateTime criacao = LocalDateTime.of(2026, 10, 1, 7, 0);
        LocalDateTime limite = criacao.plusHours(24);
        LocalDateTime expirado = limite.plusSeconds(1);
        Ocorrencia registro = new Ocorrencia();
        registro.setMatricula(matricula);
        registro.setMembro(membro);
        registro.setMembro(membro);
        registro.setDataCriacao(criacao);
        registro.setDataOcorrencia(criacao.withHour(9));
        AtualizarOcorrenciaDTO dto = new AtualizarOcorrenciaDTO();
        dto.setDescricao("Correção");
        dto.setTipoOcorrencia(TipoOcorrencia.SAUDE);
        when(ocorrencias.findById(1)).thenReturn(Optional.of(registro));
        when(ocorrencias.save(registro)).thenReturn(registro);
        try (MockedStatic<LocalDateTime> tempo = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(limite);
            assertThat(service.atualizarOcorrencia(1, dto, membro).getDescricao()).isEqualTo("Correção");
            assertThatCode(() -> service.deletarOcorrencia(1, membro)).doesNotThrowAnyException();
            tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(expirado);
            assertThatThrownBy(() -> service.atualizarOcorrencia(1, dto, membro))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
            assertThatThrownBy(() -> service.deletarOcorrencia(1, membro))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        }
        verify(ocorrencias, times(1)).save(registro);
        verify(ocorrencias, times(1)).delete(registro);
    }

    @Test
    void devePermitirEditarEExcluirOcorrenciaAntigaParaCoordenadorEAdministrador() {
        Ocorrencia registro = new Ocorrencia();
        registro.setMatricula(matricula);
        registro.setMembro(membro);
        registro.setMembro(membro);
        registro.setDataCriacao(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusMonths(1));
        registro.setDataOcorrencia(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusMonths(1).withHour(9));
        AtualizarOcorrenciaDTO dto = new AtualizarOcorrenciaDTO();
        dto.setDescricao("Correção retroativa");
        dto.setTipoOcorrencia(TipoOcorrencia.SAUDE);
        when(ocorrencias.findById(1)).thenReturn(Optional.of(registro));
        when(ocorrencias.save(registro)).thenReturn(registro);
        for (String perfil : List.of("COORDENADOR", "ADMINISTRADOR")) {
            membro.getPapel().setNomePapel(perfil);
            assertThat(service.atualizarOcorrencia(1, dto, membro).getDescricao()).isEqualTo("Correção retroativa");
            assertThatCode(() -> service.deletarOcorrencia(1, membro)).doesNotThrowAnyException();
        }
    }
}
