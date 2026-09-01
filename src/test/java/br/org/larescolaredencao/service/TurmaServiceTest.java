package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.AtualizarTurmaDTO;
import br.org.larescolaredencao.dto.CriarTurmaDTO;
import br.org.larescolaredencao.model.Turma;
import br.org.larescolaredencao.model.Unidade;
import br.org.larescolaredencao.model.enums.Periodo;
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

    private CriarTurmaDTO criarDto(Integer unidadeId, Periodo periodo, LocalTime inicio, LocalTime fim) {
        CriarTurmaDTO dto = new CriarTurmaDTO();
        dto.setUnidadeId(unidadeId);
        dto.setPeriodo(periodo);
        dto.setHoraInicio(inicio);
        dto.setHoraFim(fim);
        return dto;
    }

    private Turma turmaExistente(Integer id, Integer unidadeId, Periodo periodo, LocalTime inicio, LocalTime fim) {
        Unidade unidade = new Unidade();
        unidade.setId(unidadeId);
        unidade.setNome("Unidade X");

        Turma turma = new Turma();
        turma.setId(id);
        turma.setUnidade(unidade);
        turma.setPeriodo(periodo);
        turma.setHoraInicio(inicio);
        turma.setHoraFim(fim);
        return turma;
    }

    @Test
    void deveRejeitarUnidadeInexistente() {
        when(unidadeRepository.findById(anyInt())).thenReturn(Optional.empty());

        CriarTurmaDTO dto = criarDto(99, Periodo.TARDE, LocalTime.of(14, 0), LocalTime.of(16, 0));

        assertThatThrownBy(() -> turmaService.criarTurma(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unidade não encontrada");
    }

    @Test
    void deveRejeitarHoraInicioMaiorOuIgualHoraFim() {
        when(unidadeRepository.findById(1)).thenReturn(Optional.of(new Unidade()));

        CriarTurmaDTO dto = criarDto(1, Periodo.TARDE, LocalTime.of(16, 0), LocalTime.of(16, 0));

        assertThatThrownBy(() -> turmaService.criarTurma(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("horaInicio");
    }

    @Test
    void deveRejeitarSegundaTurmaNoMesmoPeriodoDaUnidade() {
        Unidade unidade = new Unidade();
        unidade.setId(1);
        when(unidadeRepository.findById(1)).thenReturn(Optional.of(unidade));

        // mesma unidade, mesmo período (TARDE), horários que nem se sobrepõem —
        // ainda assim é conflito, porque a unidade só pode ter uma turma por período.
        Turma existente = turmaExistente(1, 1, Periodo.TARDE, LocalTime.of(13, 0), LocalTime.of(15, 0));
        when(turmaRepository.findByUnidadeId(1)).thenReturn(List.of(existente));

        CriarTurmaDTO dto = criarDto(1, Periodo.TARDE, LocalTime.of(18, 0), LocalTime.of(20, 0));

        assertThatThrownBy(() -> turmaService.criarTurma(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("já possui uma turma no período");
    }

    @Test
    void devePermitirUmaTurmaDeManhaEUmaDeTardeNaMesmaUnidade() {
        Unidade unidade = new Unidade();
        unidade.setId(1);
        when(unidadeRepository.findById(1)).thenReturn(Optional.of(unidade));

        Turma existente = turmaExistente(1, 1, Periodo.MANHA, LocalTime.of(8, 0), LocalTime.of(10, 0));
        when(turmaRepository.findByUnidadeId(1)).thenReturn(List.of(existente));
        when(turmaRepository.save(any(Turma.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CriarTurmaDTO dto = criarDto(1, Periodo.TARDE, LocalTime.of(14, 0), LocalTime.of(16, 0));

        assertThat(turmaService.criarTurma(dto)).isNotNull();
    }

    @Test
    void deveRejeitarHorarioSobrepostoEntrePeriodosDiferentes() {
        Unidade unidade = new Unidade();
        unidade.setId(1);
        when(unidadeRepository.findById(1)).thenReturn(Optional.of(unidade));

        // períodos diferentes, mas os horários de fato se cruzam.
        Turma existente = turmaExistente(1, 1, Periodo.MANHA, LocalTime.of(8, 0), LocalTime.of(14, 0));
        when(turmaRepository.findByUnidadeId(1)).thenReturn(List.of(existente));

        CriarTurmaDTO dto = criarDto(1, Periodo.TARDE, LocalTime.of(12, 0), LocalTime.of(16, 0));

        assertThatThrownBy(() -> turmaService.criarTurma(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Conflito de horário");
    }

    @Test
    void devePermitirHorariosApenasEncostadosEntrePeriodosDiferentes() {
        Unidade unidade = new Unidade();
        unidade.setId(1);
        when(unidadeRepository.findById(1)).thenReturn(Optional.of(unidade));

        Turma existente = turmaExistente(1, 1, Periodo.MANHA, LocalTime.of(8, 0), LocalTime.of(12, 0));
        when(turmaRepository.findByUnidadeId(1)).thenReturn(List.of(existente));
        when(turmaRepository.save(any(Turma.class))).thenAnswer(invocation -> {
            Turma t = invocation.getArgument(0);
            t.setId(2);
            return t;
        });

        CriarTurmaDTO dto = criarDto(1, Periodo.TARDE, LocalTime.of(12, 0), LocalTime.of(16, 0));

        assertThat(turmaService.criarTurma(dto).getId()).isEqualTo(2);
    }

    @Test
    void deveIgnorarAPropriaTurmaAoChecarConflitoNaAtualizacao() {
        Unidade unidade = new Unidade();
        unidade.setId(1);
        when(unidadeRepository.findById(1)).thenReturn(Optional.of(unidade));

        Turma turmaAtual = turmaExistente(1, 1, Periodo.TARDE, LocalTime.of(14, 0), LocalTime.of(16, 0));
        when(turmaRepository.findById(1)).thenReturn(Optional.of(turmaAtual));
        when(turmaRepository.findByUnidadeIdAndIdNot(1, 1)).thenReturn(List.of());
        when(turmaRepository.save(any(Turma.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AtualizarTurmaDTO dto = new AtualizarTurmaDTO();
        dto.setUnidadeId(1);
        dto.setPeriodo(Periodo.TARDE);
        dto.setHoraInicio(LocalTime.of(14, 0));
        dto.setHoraFim(LocalTime.of(17, 0));

        assertThat(turmaService.atualizarTurma(1, dto).getHoraFim()).isEqualTo(LocalTime.of(17, 0));
    }
}
