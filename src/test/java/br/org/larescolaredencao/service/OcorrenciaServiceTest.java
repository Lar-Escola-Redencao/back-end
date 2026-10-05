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
        matricula = new Matricula();
        matricula.setId(10);
        matricula.setTurma(turma);
        matricula.setDataIngresso(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusMonths(2));
        Papel papel = new Papel();
        papel.setNomePapel("MONITOR");
        membro = new Membro();
        membro.setPapel(papel);
        membro.setUnidades(List.of(unidade));
    }

    private CriarOcorrenciaDTO dto(LocalDate data) {
        CriarOcorrenciaDTO dto = new CriarOcorrenciaDTO();
        dto.setIdMatricula(10);
        dto.setDataOcorrencia(data);
        dto.setDescricao("Registro de acompanhamento");
        dto.setTipoOcorrencia(TipoOcorrencia.ASSISTENCIA);
        return dto;
    }

    @Test
    void deveBloquearCriacaoHa8DiasParaMonitorEPermitirCoordenador() {
        when(matriculas.findById(10)).thenReturn(Optional.of(matricula));
        CriarOcorrenciaDTO dto = dto(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalDate().minusDays(8));
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
        assertThat(service.criarOcorrencia(dto(data), membro).getDataOcorrencia()).isEqualTo(data);
    }

    @Test
    void deveUsarCriacaoDoBancoParaBloquearEdicaoEExclusaoApos24Horas() {
        Ocorrencia registro = new Ocorrencia();
        registro.setMatricula(matricula);
        registro.setDataCriacao(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusHours(24).minusMinutes(30));
        registro.setDataOcorrencia(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalDate());
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
        registro.setDataCriacao(criacao);
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
        registro.setDataCriacao(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusMonths(1));
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
