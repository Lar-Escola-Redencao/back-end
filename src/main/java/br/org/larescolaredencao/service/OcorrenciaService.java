package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.AtualizarOcorrenciaDTO;
import br.org.larescolaredencao.dto.CriarOcorrenciaDTO;
import br.org.larescolaredencao.dto.OcorrenciaResponseDTO;
import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Ocorrencia;
import br.org.larescolaredencao.model.Unidade;
import br.org.larescolaredencao.model.enums.Perfil;
import br.org.larescolaredencao.model.enums.StatusMatricula;
import br.org.larescolaredencao.repository.MatriculaRepository;
import br.org.larescolaredencao.repository.OcorrenciaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class OcorrenciaService {

    private final OcorrenciaRepository ocorrenciaRepository;
    private final MatriculaRepository matriculaRepository;

    public OcorrenciaService(OcorrenciaRepository ocorrenciaRepository, MatriculaRepository matriculaRepository) {
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.matriculaRepository = matriculaRepository;
    }

    @Transactional
    public OcorrenciaResponseDTO criarOcorrencia(CriarOcorrenciaDTO dto, Membro membroLogado) {
        Matricula matricula = matriculaRepository.findById(dto.getIdMatricula())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula não encontrada."));
        
        if (matricula.getStatus() == StatusMatricula.EXCLUIDO || matricula.getStatus() == StatusMatricula.EGRESSO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível registrar ocorrência para usuário inativo ou excluído.");
        }

        validarAcessoUnidade(membroLogado, matricula.getTurma().getUnidade());

        if (isMonitor(membroLogado)) {
            if (ChronoUnit.DAYS.between(dto.getDataOcorrencia(), LocalDate.now()) > 7) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Monitores não podem registrar ocorrências retroativas passando de 7 dias.");
            }
        }

        Ocorrencia ocorrencia = new Ocorrencia();
        ocorrencia.setMatricula(matricula);
        ocorrencia.setDataOcorrencia(dto.getDataOcorrencia());
        ocorrencia.setDescricao(dto.getDescricao());
        ocorrencia.setTipoOcorrencia(dto.getTipoOcorrencia());
        ocorrencia.setMembro(membroLogado);
        
        ocorrencia = ocorrenciaRepository.save(ocorrencia);
        return new OcorrenciaResponseDTO(ocorrencia);
    }

    @Transactional
    public OcorrenciaResponseDTO atualizarOcorrencia(Integer id, AtualizarOcorrenciaDTO dto, Membro membroLogado) {
        Ocorrencia ocorrencia = ocorrenciaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ocorrência não encontrada."));
        
        if (ocorrencia.getMatricula().getStatus() == StatusMatricula.EXCLUIDO || ocorrencia.getMatricula().getStatus() == StatusMatricula.EGRESSO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível alterar ocorrência de usuário inativo ou excluído.");
        }

        validarAcessoUnidade(membroLogado, ocorrencia.getMatricula().getTurma().getUnidade());

        if (isMonitor(membroLogado)) {
            if (ChronoUnit.HOURS.between(ocorrencia.getDataCriacao(), LocalDateTime.now()) > 24) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Monitores não podem alterar ocorrências criadas há mais de 24 horas.");
            }
        }

        ocorrencia.setDescricao(dto.getDescricao());
        ocorrencia.setTipoOcorrencia(dto.getTipoOcorrencia());
        ocorrencia.setMembro(membroLogado);
        ocorrencia = ocorrenciaRepository.save(ocorrencia);
        
        return new OcorrenciaResponseDTO(ocorrencia);
    }

    @Transactional
    public void deletarOcorrencia(Integer id, Membro membroLogado) {
        Ocorrencia ocorrencia = ocorrenciaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ocorrência não encontrada."));

        validarAcessoUnidade(membroLogado, ocorrencia.getMatricula().getTurma().getUnidade());

        if (isMonitor(membroLogado)) {
            if (ChronoUnit.HOURS.between(ocorrencia.getDataCriacao(), LocalDateTime.now()) > 24) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Monitores não podem excluir ocorrências criadas há mais de 24 horas.");
            }
        }

        ocorrenciaRepository.delete(ocorrencia);
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