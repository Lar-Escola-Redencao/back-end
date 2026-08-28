package br.org.larescolaredencao.service;

import java.time.LocalTime;
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
        validarHorario(dto.getHoraInicio(), dto.getHoraFim());
        validarConflito(dto.getUnidadeId(), null, dto.getHoraInicio(), dto.getHoraFim());

        Turma turma = new Turma();
        turma.setPeriodo(dto.getPeriodo());
        turma.setUnidade(unidade);
        turma.setHoraInicio(dto.getHoraInicio());
        turma.setHoraFim(dto.getHoraFim());

        return new TurmaResponseDTO(turmaRepository.save(turma));
    }

    public TurmaResponseDTO atualizarTurma(Integer id, AtualizarTurmaDTO dto) {
        Turma turma = buscarTurmaOuFalhar(id);
        Unidade unidade = buscarUnidadeOuFalhar(dto.getUnidadeId());
        validarHorario(dto.getHoraInicio(), dto.getHoraFim());
        validarConflito(dto.getUnidadeId(), id, dto.getHoraInicio(), dto.getHoraFim());

        turma.setPeriodo(dto.getPeriodo());
        turma.setUnidade(unidade);
        turma.setHoraInicio(dto.getHoraInicio());
        turma.setHoraFim(dto.getHoraFim());

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

    private void validarHorario(LocalTime horaInicio, LocalTime horaFim) {
        if (!horaInicio.isBefore(horaFim)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "horaInicio deve ser anterior a horaFim.");
        }
    }

    private void validarConflito(Integer unidadeId, Integer idTurmaAtual, LocalTime horaInicio, LocalTime horaFim) {
        List<Turma> turmasDaUnidade = idTurmaAtual == null
                ? turmaRepository.findByUnidadeId(unidadeId)
                : turmaRepository.findByUnidadeIdAndIdNot(unidadeId, idTurmaAtual);

        for (Turma outra : turmasDaUnidade) {
            boolean horarioSobrepoe = horaInicio.isBefore(outra.getHoraFim()) && outra.getHoraInicio().isBefore(horaFim);
            if (horarioSobrepoe) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Conflito de horário com a turma id=" + outra.getId() + ".");
            }
        }
    }
}
