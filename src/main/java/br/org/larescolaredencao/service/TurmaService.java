package br.org.larescolaredencao.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.AtualizarTurmaDTO;
import br.org.larescolaredencao.dto.CriarTurmaDTO;
import br.org.larescolaredencao.dto.TurmaResponseDTO;
import br.org.larescolaredencao.model.Turma;
import br.org.larescolaredencao.model.Unidade;
import br.org.larescolaredencao.model.enums.DiaSemana;
import br.org.larescolaredencao.repository.TurmaRepository;
import br.org.larescolaredencao.repository.UnidadeRepository;

@Service
public class TurmaService {

    private final TurmaRepository turmaRepository;
    private final UnidadeRepository unidadeRepository;

    public TurmaService(TurmaRepository turmaRepository, UnidadeRepository unidadeRepository) {
        this.turmaRepository = turmaRepository;
        this.unidadeRepository = unidadeRepository;
    }

    public List<TurmaResponseDTO> listarTurmas(Integer unidadeId) {
        List<Turma> turmas = unidadeId != null
                ? turmaRepository.findByUnidadeId(unidadeId)
                : turmaRepository.findAll();

        return turmas.stream().map(TurmaResponseDTO::new).collect(Collectors.toList());
    }

    public TurmaResponseDTO buscarTurma(Integer id) {
        return new TurmaResponseDTO(buscarTurmaOuFalhar(id));
    }

    public TurmaResponseDTO criarTurma(CriarTurmaDTO dto) {
        Unidade unidade = buscarUnidadeOuFalhar(dto.getUnidadeId());
        validarPeriodo(dto.getDataInicio(), dto.getDataFim(), dto.getHoraInicio(), dto.getHoraFim());
        validarConflito(dto.getUnidadeId(), null, dto.getDiasSemana(), dto.getDataInicio(), dto.getDataFim(),
                dto.getHoraInicio(), dto.getHoraFim());

        Turma turma = new Turma();
        turma.setNome(dto.getNome());
        turma.setUnidade(unidade);
        turma.setDiasSemana(dto.getDiasSemana());
        turma.setHoraInicio(dto.getHoraInicio());
        turma.setHoraFim(dto.getHoraFim());
        turma.setDataInicio(dto.getDataInicio());
        turma.setDataFim(dto.getDataFim());
        turma.setVagas(dto.getVagas());

        return new TurmaResponseDTO(turmaRepository.save(turma));
    }

    public TurmaResponseDTO atualizarTurma(Integer id, AtualizarTurmaDTO dto) {
        Turma turma = buscarTurmaOuFalhar(id);
        Unidade unidade = buscarUnidadeOuFalhar(dto.getUnidadeId());
        validarPeriodo(dto.getDataInicio(), dto.getDataFim(), dto.getHoraInicio(), dto.getHoraFim());
        validarConflito(dto.getUnidadeId(), id, dto.getDiasSemana(), dto.getDataInicio(), dto.getDataFim(),
                dto.getHoraInicio(), dto.getHoraFim());

        turma.setNome(dto.getNome());
        turma.setUnidade(unidade);
        turma.setDiasSemana(dto.getDiasSemana());
        turma.setHoraInicio(dto.getHoraInicio());
        turma.setHoraFim(dto.getHoraFim());
        turma.setDataInicio(dto.getDataInicio());
        turma.setDataFim(dto.getDataFim());
        turma.setVagas(dto.getVagas());

        return new TurmaResponseDTO(turmaRepository.save(turma));
    }

    public void deletarTurma(Integer id) {
        Turma turma = buscarTurmaOuFalhar(id);
        turmaRepository.delete(turma);
    }

    private Turma buscarTurmaOuFalhar(Integer id) {
        return turmaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada."));
    }

    private Unidade buscarUnidadeOuFalhar(Integer unidadeId) {
        return unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "Unidade não encontrada."));
    }

    private void validarPeriodo(LocalDate dataInicio, LocalDate dataFim, LocalTime horaInicio, LocalTime horaFim) {
        if (dataInicio.isAfter(dataFim)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dataInicio não pode ser posterior a dataFim.");
        }
        if (!horaInicio.isBefore(horaFim)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "horaInicio deve ser anterior a horaFim.");
        }
    }

    private void validarConflito(Integer unidadeId, Integer idTurmaAtual, List<DiaSemana> diasSemana,
            LocalDate dataInicio, LocalDate dataFim, LocalTime horaInicio, LocalTime horaFim) {

        List<Turma> turmasDaUnidade = idTurmaAtual == null
                ? turmaRepository.findByUnidadeId(unidadeId)
                : turmaRepository.findByUnidadeIdAndIdNot(unidadeId, idTurmaAtual);

        for (Turma outra : turmasDaUnidade) {
            boolean mesmoDia = !Collections.disjoint(outra.getDiasSemana(), diasSemana);
            if (!mesmoDia) {
                continue;
            }

            boolean periodoSobrepoe = !dataInicio.isAfter(outra.getDataFim()) && !outra.getDataInicio().isAfter(dataFim);
            if (!periodoSobrepoe) {
                continue;
            }

            boolean horarioSobrepoe = horaInicio.isBefore(outra.getHoraFim()) && outra.getHoraInicio().isBefore(horaFim);
            if (horarioSobrepoe) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Conflito de horário com a turma '" + outra.getNome() + "' (id=" + outra.getId() + ").");
            }
        }
    }
}
