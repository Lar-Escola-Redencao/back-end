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
import br.org.larescolaredencao.model.enums.StatusMatricula;
import br.org.larescolaredencao.repository.FrequenciaRepository;
import br.org.larescolaredencao.repository.MatriculaRepository;
import br.org.larescolaredencao.repository.OcorrenciaRepository;
import br.org.larescolaredencao.repository.TurmaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
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

        LocalDateTime dataFimDia = data.plusDays(1).atStartOfDay();
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
                    
            return montarRespostaUsuario(matricula, freq, ocs, membroLogado);
        }).collect(Collectors.toList());
    }

    @Transactional
    public void salvarFrequenciaEmLote(SalvarFrequenciaEmLoteDTO dto, Membro membroLogado) {
        Turma turma = turmaRepository.findById(dto.getIdTurma())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada."));

        validarAcessoUnidade(membroLogado, turma.getUnidade());

        registrarEmLote(dto, membroLogado, turma, false);
    }

    @Transactional
    public void atualizarFrequenciaEmLote(SalvarFrequenciaEmLoteDTO dto, Membro membroLogado) {
        Turma turma = turmaRepository.findById(dto.getIdTurma())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada."));
        validarAcessoUnidade(membroLogado, turma.getUnidade());
        registrarEmLote(dto, membroLogado, turma, true);
    }

    private void registrarEmLote(SalvarFrequenciaEmLoteDTO dto, Membro membroLogado, Turma turma, boolean apenasAtualizar) {
        LocalDateTime agora = agora();
        LocalDateTime inicio = LocalDateTime.of(dto.getData(), turma.getHoraInicio());
        if (isMonitor(membroLogado) && agora.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não é possível registrar a chamada antes do horário de início da turma.");
        }
        if (apenasAtualizar) {
            validarEdicaoFrequencia(membroLogado, inicio, agora);
        }

        Set<Integer> ids = new HashSet<>();
        List<Frequencia> registros = new ArrayList<>();
        for (FrequenciaDTO fDto : dto.getFrequencias()) {
            if (!ids.add(fDto.getIdMatricula())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matrícula repetida no lote.");
            }
            Matricula matricula = matriculaRepository.findById(fDto.getIdMatricula())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Matrícula não encontrada: " + fDto.getIdMatricula()));
            validarAcessoUnidade(membroLogado, matricula.getTurma().getUnidade());
            if (!matricula.getTurma().getId().equals(turma.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A matrícula não pertence à turma informada.");
            }
            validarMatriculaNaData(matricula, dto.getData());
            Frequencia frequencia = frequenciaRepository.findByMatriculaIdAndDataRegistro(matricula.getId(), dto.getData())
                    .orElse(null);
            if (frequencia != null) {
                validarEdicaoFrequencia(membroLogado, inicio, agora);
            } else {
                if (apenasAtualizar) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de frequência não encontrado.");
                }
                frequencia = new Frequencia();
                frequencia.setMatricula(matricula);
                frequencia.setDataRegistro(dto.getData());
            }
            frequencia.setPresente(fDto.getPresente());
            frequencia.setMembro(membroLogado);
            registros.add(frequencia);
        }
        try {
            frequenciaRepository.saveAllAndFlush(registros);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A chamada conflita com registros existentes. Recarregue o diário antes de salvar.", exception);
        }
    }

    @Transactional
    public FrequenciaUsuarioResponseDTO atualizarFrequencia(Integer id, AtualizarFrequenciaDTO dto, Membro membroLogado) {
        Frequencia frequencia = frequenciaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de frequência não encontrado."));
        
        Turma turma = frequencia.getMatricula().getTurma();
        validarAcessoUnidade(membroLogado, turma.getUnidade());
        validarMatriculaNaData(frequencia.getMatricula(), frequencia.getDataRegistro());
        validarEdicaoFrequencia(membroLogado,
                LocalDateTime.of(frequencia.getDataRegistro(), turma.getHoraInicio()), agora());

        frequencia.setPresente(dto.getPresente());
        frequencia.setMembro(membroLogado);
        frequencia = frequenciaRepository.save(frequencia);

        List<Ocorrencia> ocorrencias = ocorrenciaRepository.findByMatriculaIdAndDataOcorrencia(frequencia.getMatricula().getId(), frequencia.getDataRegistro());
        return montarRespostaUsuario(frequencia.getMatricula(), frequencia, ocorrencias, membroLogado);
    }

    private FrequenciaUsuarioResponseDTO montarRespostaUsuario(Matricula matricula, Frequencia frequencia,
                                                               List<Ocorrencia> ocorrencias, Membro membroLogado) {
        boolean usuarioExcluido = usuarioTemMatriculaExcluida(matricula);
        boolean permiteAcessoPerfil = permiteAcessoPerfil(matricula, membroLogado);
        return new FrequenciaUsuarioResponseDTO(matricula, frequencia, ocorrencias, permiteAcessoPerfil, usuarioExcluido);
    }

    private boolean usuarioTemMatriculaExcluida(Matricula matricula) {
        return matricula.getUsuario().getMatriculas() != null
                && matricula.getUsuario().getMatriculas().stream()
                        .anyMatch(m -> m.getStatus() == StatusMatricula.EXCLUIDO);
    }

    private boolean permiteAcessoPerfil(Matricula matricula, Membro membroLogado) {
        if (matricula.getStatus() == StatusMatricula.EXCLUIDO || usuarioTemMatriculaExcluida(matricula)) {
            return false;
        }
        if (Perfil.fromNomePapel(membroLogado.getPapel().getNomePapel()) == Perfil.ADMINISTRADOR) {
            return true;
        }

        Set<Integer> unidadesPermitidas = membroLogado.getUnidades().stream()
                .map(Unidade::getId)
                .collect(Collectors.toSet());
        return matricula.getUsuario().getMatriculas() != null
                && matricula.getUsuario().getMatriculas().stream()
                        .filter(m -> m.getStatus() != StatusMatricula.EXCLUIDO)
                        .map(Matricula::getTurma)
                        .filter(turma -> turma != null && turma.getUnidade() != null)
                        .map(turma -> turma.getUnidade().getId())
                        .anyMatch(unidadesPermitidas::contains);
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
    }

    private boolean isMonitor(Membro membro) {
        return Perfil.fromNomePapel(membro.getPapel().getNomePapel()) == Perfil.MONITOR;
    }

    private void validarAcessoUnidade(Membro membro, Unidade unidade) {
        if (Perfil.fromNomePapel(membro.getPapel().getNomePapel()) == Perfil.ADMINISTRADOR) {
            return;
        }
        if (membro.getUnidades().stream().noneMatch(u -> u.getId().equals(unidade.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "O usuário não possui permissão de acesso a esta unidade.");
        }
    }

    private void validarMatriculaNaData(Matricula matricula, LocalDate data) {
        if (matricula.getDataIngresso().toLocalDate().isAfter(data)
                || (matricula.getDataDesligamento() != null && matricula.getDataDesligamento().isBefore(data))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A matrícula não estava ativa na data informada.");
        }
    }

    private void validarEdicaoFrequencia(Membro membro, LocalDateTime referencia, LocalDateTime agora) {
        if (isMonitor(membro) && agora.isAfter(referencia.plusHours(48))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Monitores não podem alterar frequências registradas há mais de 48 horas.");
        }
    }
}
