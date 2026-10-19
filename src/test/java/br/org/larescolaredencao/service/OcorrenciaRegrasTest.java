package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.*;
import br.org.larescolaredencao.model.*;
import br.org.larescolaredencao.model.enums.TipoOcorrencia;
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
class OcorrenciaRegrasTest {
    @Mock OcorrenciaRepository ocorrencias;
    @Mock MatriculaRepository matriculas;
    OcorrenciaService service;
    Matricula matricula;
    Membro monitor;
    final LocalDateTime agora = LocalDateTime.of(2026, 10, 31, 15, 0);

    @BeforeEach void preparar() {
        service = new OcorrenciaService(ocorrencias, matriculas);
        Unidade unidade = new Unidade(); unidade.setId(1);
        Turma turma = new Turma(); turma.setId(1); turma.setUnidade(unidade);
        turma.setHoraInicio(LocalTime.of(8, 0)); turma.setHoraFim(LocalTime.of(18, 0));
        matricula = new Matricula(); matricula.setId(10); matricula.setTurma(turma);
        matricula.setDataIngresso(LocalDateTime.of(2026, 8, 1, 0, 0));
        monitor = colaborador(7, "MONITOR");
    }

    Membro colaborador(int id, String papel) {
        Membro membro = new Membro(); membro.setId(id); membro.setNomeCompleto("Autor " + id);
        membro.setPapel(new Papel(3, papel, ""));
        membro.setUnidades(List.of(matricula.getTurma().getUnidade()));
        return membro;
    }

    CriarOcorrenciaDTO cadastro(LocalDateTime fato) {
        CriarOcorrenciaDTO dto = new CriarOcorrenciaDTO(); dto.setIdMatricula(10);
        dto.setDataOcorrencia(fato); dto.setDescricao("Fato"); dto.setTipoOcorrencia(TipoOcorrencia.OUTRO);
        return dto;
    }

    Ocorrencia registro() {
        Ocorrencia oc = new Ocorrencia(); oc.setId(1); oc.setMatricula(matricula); oc.setMembro(monitor);
        oc.setDataCriacao(agora); oc.setDataOcorrencia(LocalDateTime.of(2026, 9, 1, 9, 0));
        oc.setDescricao("Original"); oc.setTipoOcorrencia(TipoOcorrencia.OUTRO);
        return oc;
    }

    AtualizarOcorrenciaDTO edicao(LocalDateTime fato) {
        AtualizarOcorrenciaDTO dto = new AtualizarOcorrenciaDTO(); dto.setDescricao("Corrigido");
        dto.setTipoOcorrencia(TipoOcorrencia.SAUDE); dto.setDataOcorrencia(fato); return dto;
    }

    void proibido(Runnable operacao) {
        assertThatThrownBy(operacao::run).isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test void janelaMensalSubstituiSeteDiasComPrimeiroDiaInclusivo() {
        when(matriculas.findById(10)).thenReturn(Optional.of(matricula));
        when(ocorrencias.save(any())).thenAnswer(inv -> inv.getArgument(0));
        try (var tempo = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(agora);
            assertThat(service.criarOcorrencia(cadastro(LocalDateTime.of(2026, 9, 1, 8, 0)), monitor).getDataCriacao()).isEqualTo(agora);
            proibido(() -> service.criarOcorrencia(cadastro(LocalDateTime.of(2026, 8, 31, 8, 0)), monitor));
        }
        verify(ocorrencias, times(1)).save(any());
    }

    @Test void monitorNaoPodeAnteciparAulaOuHorarioDoFato() {
        when(matriculas.findById(10)).thenReturn(Optional.of(matricula));
        try (var tempo = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(agora.withHour(7));
            proibido(() -> service.criarOcorrencia(cadastro(agora.withHour(8)), monitor));
            tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(agora);
            proibido(() -> service.criarOcorrencia(cadastro(agora.withHour(16)), monitor));
        }
        verifyNoInteractions(ocorrencias);
    }

    @Test void outroMonitorERegistroDeAdministradorNaoPodemSerEditadosOuExcluidos() {
        Ocorrencia oc = registro();
        when(ocorrencias.findById(1)).thenReturn(Optional.of(oc));
        try (var tempo = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(agora);
            for (String papel : List.of("MONITOR", "ADMINISTRADOR", "COORDENADOR")) {
                oc.setMembro(colaborador(8, papel));
                proibido(() -> service.atualizarOcorrencia(1, edicao(oc.getDataOcorrencia()), monitor));
                proibido(() -> service.deletarOcorrencia(1, monitor));
            }
        }
        assertThat(oc.getDescricao()).isEqualTo("Original");
        verify(ocorrencias, never()).save(any()); verify(ocorrencias, never()).delete(any());
    }

    @Test void viradaDoMesPermiteCorrecaoAte24hSemTrocarAutorOuCriacao() {
        Ocorrencia oc = registro();
        when(ocorrencias.findById(1)).thenReturn(Optional.of(oc));
        when(ocorrencias.save(oc)).thenReturn(oc);
        LocalDateTime fatoCorrigido = oc.getDataOcorrencia().withHour(10);
        try (var tempo = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(agora.plusHours(24));
            service.atualizarOcorrencia(1, edicao(fatoCorrigido), monitor);
            service.deletarOcorrencia(1, monitor);
            tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(agora.plusHours(24).plusSeconds(1));
            proibido(() -> service.atualizarOcorrencia(1, edicao(fatoCorrigido), monitor));
            proibido(() -> service.deletarOcorrencia(1, monitor));
        }
        assertThat(oc.getDataCriacao()).isEqualTo(agora);
        assertThat(oc.getMembro()).isSameAs(monitor);
    }

    @Test void administradorECoordenadorNaoSubstituemAutorAoCorrigir() {
        Ocorrencia oc = registro(); oc.setDataCriacao(agora.minusDays(10));
        when(ocorrencias.findById(1)).thenReturn(Optional.of(oc));
        when(ocorrencias.save(oc)).thenReturn(oc);
        for (String papel : List.of("ADMINISTRADOR", "COORDENADOR")) {
            var resposta = service.atualizarOcorrencia(1, edicao(oc.getDataOcorrencia()), colaborador(8, papel));
            assertThat(resposta.getNomeMembro()).isEqualTo("Autor 7");
            assertThat(oc.getMembro()).isSameAs(monitor);
            assertThat(oc.getDataCriacao()).isEqualTo(agora.minusDays(10));
        }
    }

    @Test void editarHoraParaFuturoNaoSalvaNemMudaDescricaoOuCriacao() {
        Ocorrencia oc = registro(); oc.setDataOcorrencia(agora.withHour(9));
        when(ocorrencias.findById(1)).thenReturn(Optional.of(oc));
        try (var tempo = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            tempo.when(() -> LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))).thenReturn(agora);
            proibido(() -> service.atualizarOcorrencia(1, edicao(agora.withHour(16)), monitor));
        }
        assertThat(oc.getDataOcorrencia()).isEqualTo(agora.withHour(9));
        assertThat(oc.getDescricao()).isEqualTo("Original");
        assertThat(oc.getDataCriacao()).isEqualTo(agora);
        verify(ocorrencias, never()).save(any());
    }

    @Test void acessoAOutraUnidadeNaoPermiteCriarEditarOuExcluir() {
        Ocorrencia oc = registro();
        when(ocorrencias.findById(1)).thenReturn(Optional.of(oc));
        when(matriculas.findById(10)).thenReturn(Optional.of(matricula));
        monitor.setUnidades(List.of());
        proibido(() -> service.criarOcorrencia(cadastro(oc.getDataOcorrencia()), monitor));
        proibido(() -> service.atualizarOcorrencia(1, edicao(oc.getDataOcorrencia()), monitor));
        proibido(() -> service.deletarOcorrencia(1, monitor));
        verify(ocorrencias, never()).save(any()); verify(ocorrencias, never()).delete(any());
    }
}
