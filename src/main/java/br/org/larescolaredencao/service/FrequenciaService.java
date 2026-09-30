package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.AtualizarFrequenciaDTO;
import br.org.larescolaredencao.dto.FrequenciaUsuarioResponseDTO;
import br.org.larescolaredencao.dto.FrequenciaDTO;
import br.org.larescolaredencao.dto.SalvarFrequenciaEmLoteDTO;
import br.org.larescolaredencao.model.Frequencia;
import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Ocorrencia;
import br.org.larescolaredencao.model.Turma;
import br.org.larescolaredencao.model.Unidade;
import br.org.larescolaredencao.model.enums.Perfil;
import br.org.larescolaredencao.repository.FrequenciaRepository;
import br.org.larescolaredencao.repository.MatriculaRepository;
import br.org.larescolaredencao.repository.OcorrenciaRepository;
import br.org.larescolaredencao.repository.TurmaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FrequenciaService {

    private final FrequenciaRepository frequenciaRepository;
    private final MatriculaRepository matriculaRepository;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final TurmaRepository turmaRepository;

    public FrequenciaService(FrequenciaRepository frequenciaRepository,
                             MatriculaRepository matriculaRepository,
                             OcorrenciaRepository ocorrenciaRepository,
                             TurmaRepository turmaRepository) {
        this.frequenciaRepository = frequenciaRepository;
        this.matriculaRepository = matriculaRepository;
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.turmaRepository = turmaRepository;
    }

    @Transactional(readOnly = true)
    public List<FrequenciaUsuarioResponseDTO> listarFrequencia(Integer turmaId, LocalDate data, Membro membroLogado) {
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada."));

        validarAcessoUnidade(membroLogado, turma.getUnidade());

        LocalDateTime dataFimDia = data.atTime(23, 59, 59);
        List<Matricula> matriculasHistoricas = matriculaRepository.findHistoricoAtivasPorTurmaEData(turmaId, dataFimDia, data);
        
        List<Frequencia> frequencias = frequenciaRepository.findByMatriculaTurmaIdAndDataRegistro(turmaId, data);
        List<Ocorrencia> ocorrencias = ocorrenciaRepository.findByMatriculaTurmaIdAndDataOcorrencia(turmaId, data);

        return matriculasHistoricas.stream().map(matricula -> {
            Frequencia freq = frequencias.stream()
                    .filter(f -> f.getMatricula().getId().equals(matricula.getId()))
                    .findFirst()
                    .orElse(null);
            
            List<Ocorrencia> ocs = ocorrencias.stream()
                    .filter(o -> o.getMatricula().getId().equals(matricula.getId()))
                    .collect(Collectors.toList());
                    
            return new FrequenciaUsuarioResponseDTO(matricula, freq, ocs);
        }).collect(Collectors.toList());
    }

    @Transactional
    public void salvarFrequenciaEmLote(SalvarFrequenciaEmLoteDTO dto, Membro membroLogado) {
        Turma turma = turmaRepository.findById(dto.getIdTurma())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada."));

        validarAcessoUnidade(membroLogado, turma.getUnidade());

        LocalDateTime dataHoraInicioTurma = LocalDateTime.of(dto.getData(), turma.getHoraInicio());
        if (LocalDateTime.now().isBefore(dataHoraInicioTurma)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível registrar a chamada antes do horário de início da turma.");
        }

        for (FrequenciaDTO fDto : dto.getFrequencias()) {
            Matricula matricula = matriculaRepository.findById(fDto.getIdMatricula())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula não encontrada: " + fDto.getIdMatricula()));
            
            Frequencia frequencia = frequenciaRepository.findByMatriculaIdAndDataRegistro(matricula.getId(), dto.getData())
                    .orElseGet(() -> {
                        Frequencia novaFreq = new Frequencia();
                        novaFreq.setMatricula(matricula);
                        novaFreq.setDataRegistro(dto.getData());
                        return novaFreq;
                    });
                    
            frequencia.setPresente(fDto.getPresente());
            frequencia.setMembro(membroLogado);
            frequenciaRepository.save(frequencia);
        }
    }

    @Transactional
    public FrequenciaUsuarioResponseDTO atualizarFrequencia(Integer id, AtualizarFrequenciaDTO dto, Membro membroLogado) {
        Frequencia frequencia = frequenciaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de frequência não encontrado."));

        Turma turma = frequencia.getMatricula().getTurma();
        validarAcessoUnidade(membroLogado, turma.getUnidade());

        if (isMonitor(membroLogado)) {
            LocalDateTime dataHoraInicioReferencia = LocalDateTime.of(frequencia.getDataRegistro(), turma.getHoraInicio());
            if (ChronoUnit.HOURS.between(dataHoraInicioReferencia, LocalDateTime.now()) > 48) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Monitores não podem alterar frequências registradas há mais de 48 horas.");
            }
        }

        frequencia.setPresente(dto.getPresente());
        frequencia.setMembro(membroLogado);
        frequencia = frequenciaRepository.save(frequencia);

        List<Ocorrencia> ocorrencias = ocorrenciaRepository.findByMatriculaIdAndDataOcorrencia(frequencia.getMatricula().getId(), frequencia.getDataRegistro());
        return new FrequenciaUsuarioResponseDTO(frequencia.getMatricula(), frequencia, ocorrencias);
    }

    private void validarAcessoUnidade(Membro membro, Unidade unidade) {
        boolean temAcesso = membro.getUnidades().stream().anyMatch(u -> u.getId().equals(unidade.getId()));
        if (!temAcesso) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "O usuário não possui permissão de acesso a esta unidade.");
        }
    }

    private boolean isMonitor(Membro membro) {
        return Perfil.fromNomePapel(membro.getPapel().getNomePapel()) == Perfil.MONITOR;
    }
}