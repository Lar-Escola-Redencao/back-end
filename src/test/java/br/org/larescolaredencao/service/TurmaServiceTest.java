package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.CriarTurmaDTO;
import br.org.larescolaredencao.model.Turma;
import br.org.larescolaredencao.model.Unidade;
import br.org.larescolaredencao.model.enums.DiaSemana;
import br.org.larescolaredencao.repository.TurmaRepository;
import br.org.larescolaredencao.repository.UnidadeRepository;

@ExtendWith(MockitoExtension.class)
class TurmaServiceTest {

    @Mock
    private TurmaRepository turmaRepository;

    @Mock
    private UnidadeRepository unidadeRepository;

    private TurmaService turmaService;

    @BeforeEach
    void setUp() {
        turmaService = new TurmaService(turmaRepository, unidadeRepository);
    }

    private CriarTurmaDTO criarDto(Integer unidadeId, List<DiaSemana> dias, LocalTime inicio, LocalTime fim,
            LocalDate dataInicio, LocalDate dataFim) {
        CriarTurmaDTO dto = new CriarTurmaDTO();
        dto.setNome("Informática Básica - Turma A");
        dto.setUnidadeId(unidadeId);
        dto.setDiasSemana(dias);
        dto.setHoraInicio(inicio);
        dto.setHoraFim(fim);
        dto.setDataInicio(dataInicio);
        dto.setDataFim(dataFim);
        dto.setVagas(20);
        return dto;
    }

    private Turma turmaExistente(Integer id, Integer unidadeId, List<DiaSemana> dias, LocalTime inicio, LocalTime fim,
            LocalDate dataInicio, LocalDate dataFim) {
        Unidade unidade = new Unidade();
        unidade.setId(unidadeId);
        unidade.setNome("Unidade X");

        Turma turma = new Turma();
        turma.setId(id);
        turma.setNome("Turma existente");
        turma.setUnidade(unidade);
        turma.setDiasSemana(dias);
        turma.setHoraInicio(inicio);
        turma.setHoraFim(fim);
        turma.setDataInicio(dataInicio);
        turma.setDataFim(dataFim);
        turma.setVagas(15);
        return turma;
    }

    @Test
    void deveRejeitarUnidadeInexistente() {
        when(unidadeRepository.findById(anyInt())).thenReturn(Optional.empty());

        CriarTurmaDTO dto = criarDto(99, List.of(DiaSemana.SEGUNDA), LocalTime.of(14, 0), LocalTime.of(16, 0),
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 6, 30));

        assertThatThrownBy(() -> turmaService.criarTurma(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unidade não encontrada");
    }

    @Test
    void deveRejeitarPeriodoComDataInicioAposDataFim() {
        when(unidadeRepository.findById(1)).thenReturn(Optional.of(new Unidade()));

        CriarTurmaDTO dto = criarDto(1, List.of(DiaSemana.SEGUNDA), LocalTime.of(14, 0), LocalTime.of(16, 0),
                LocalDate.of(2026, 6, 30), LocalDate.of(2026, 3, 2));

        assertThatThrownBy(() -> turmaService.criarTurma(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("dataInicio");
    }

    @Test
    void deveRejeitarHoraInicioMaiorOuIgualHoraFim() {
        when(unidadeRepository.findById(1)).thenReturn(Optional.of(new Unidade()));

        CriarTurmaDTO dto = criarDto(1, List.of(DiaSemana.SEGUNDA), LocalTime.of(16, 0), LocalTime.of(16, 0),
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 6, 30));

        assertThatThrownBy(() -> turmaService.criarTurma(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("horaInicio");
    }

    @Test
    void deveRejeitarHorarioSobrepostoNoMesmoDiaEUnidade() {
        Unidade unidade = new Unidade();
        unidade.setId(1);
        when(unidadeRepository.findById(1)).thenReturn(Optional.of(unidade));

        Turma existente = turmaExistente(1, 1, List.of(DiaSemana.SEGUNDA), LocalTime.of(14, 0), LocalTime.of(16, 0),
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 6, 30));
        when(turmaRepository.findByUnidadeId(1)).thenReturn(List.of(existente));

        CriarTurmaDTO dto = criarDto(1, List.of(DiaSemana.SEGUNDA), LocalTime.of(15, 0), LocalTime.of(17, 0),
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 6, 30));

        assertThatThrownBy(() -> turmaService.criarTurma(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Conflito de horário");
    }

    @Test
    void devePermitirHorariosApenasEncostados() {
        Unidade unidade = new Unidade();
        unidade.setId(1);
        when(unidadeRepository.findById(1)).thenReturn(Optional.of(unidade));

        Turma existente = turmaExistente(1, 1, List.of(DiaSemana.SEGUNDA), LocalTime.of(14, 0), LocalTime.of(16, 0),
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 6, 30));
        when(turmaRepository.findByUnidadeId(1)).thenReturn(List.of(existente));
        when(turmaRepository.save(any(Turma.class))).thenAnswer(invocation -> {
            Turma t = invocation.getArgument(0);
            t.setId(2);
            return t;
        });

        CriarTurmaDTO dto = criarDto(1, List.of(DiaSemana.SEGUNDA), LocalTime.of(16, 0), LocalTime.of(18, 0),
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 6, 30));

        assertThat(turmaService.criarTurma(dto).getId()).isEqualTo(2);
    }

    @Test
    void devePermitirPeriodosQueNaoSeCruzamNoCalendario() {
        Unidade unidade = new Unidade();
        unidade.setId(1);
        when(unidadeRepository.findById(1)).thenReturn(Optional.of(unidade));

        Turma existente = turmaExistente(1, 1, List.of(DiaSemana.SEGUNDA), LocalTime.of(14, 0), LocalTime.of(16, 0),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30));
        when(turmaRepository.findByUnidadeId(1)).thenReturn(List.of(existente));
        when(turmaRepository.save(any(Turma.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CriarTurmaDTO dto = criarDto(1, List.of(DiaSemana.SEGUNDA), LocalTime.of(14, 0), LocalTime.of(16, 0),
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 11, 30));

        assertThat(turmaService.criarTurma(dto)).isNotNull();
    }

    @Test
    void deveIgnorarAPropriaTurmaAoChecarConflitoNaAtualizacao() {
        Unidade unidade = new Unidade();
        unidade.setId(1);
        when(unidadeRepository.findById(1)).thenReturn(Optional.of(unidade));

        Turma turmaAtual = turmaExistente(1, 1, List.of(DiaSemana.SEGUNDA), LocalTime.of(14, 0), LocalTime.of(16, 0),
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 6, 30));
        when(turmaRepository.findById(1)).thenReturn(Optional.of(turmaAtual));
        when(turmaRepository.findByUnidadeIdAndIdNot(1, 1)).thenReturn(List.of());
        when(turmaRepository.save(any(Turma.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = new br.org.larescolaredencao.dto.AtualizarTurmaDTO();
        dto.setNome("Informática Básica - Turma A (renomeada)");
        dto.setUnidadeId(1);
        dto.setDiasSemana(List.of(DiaSemana.SEGUNDA));
        dto.setHoraInicio(LocalTime.of(14, 0));
        dto.setHoraFim(LocalTime.of(16, 0));
        dto.setDataInicio(LocalDate.of(2026, 3, 2));
        dto.setDataFim(LocalDate.of(2026, 6, 30));
        dto.setVagas(25);

        assertThat(turmaService.atualizarTurma(1, dto).getNome()).contains("renomeada");
    }
}
