package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.AtualizarOcorrenciaDTO;
import br.org.larescolaredencao.dto.CriarOcorrenciaDTO;
import br.org.larescolaredencao.dto.OcorrenciaResponseDTO;
import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Ocorrencia;
import br.org.larescolaredencao.model.Unidade;
import br.org.larescolaredencao.model.enums.Perfil;
import br.org.larescolaredencao.repository.MatriculaRepository;
import br.org.larescolaredencao.repository.OcorrenciaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

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
        
        validarAcessoUnidade(membroLogado, matricula.getTurma().getUnidade());
        LocalDateTime agora = agora();
        if (isMonitor(membroLogado) && dto.getDataOcorrencia().isBefore(agora.toLocalDate().minusDays(7))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Monitores não podem registrar ocorrências retroativas passando de 7 dias.");
        }
        if (isMonitor(membroLogado) && dto.getDataOcorrencia().isAfter(agora.toLocalDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível registrar ocorrência em data futura.");
        }
        validarMatriculaNaData(matricula, dto.getDataOcorrencia());

        Ocorrencia ocorrencia = new Ocorrencia();
        ocorrencia.setDataCriacao(agora);
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
        
        validarAcessoUnidade(membroLogado, ocorrencia.getMatricula().getTurma().getUnidade());
        validarEdicaoOcorrencia(membroLogado, ocorrencia.getDataCriacao(), agora());

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

        validarEdicaoOcorrencia(membroLogado, ocorrencia.getDataCriacao(), agora());

        ocorrenciaRepository.delete(ocorrencia);
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

    private void validarEdicaoOcorrencia(Membro membro, LocalDateTime criacao, LocalDateTime agora) {
        if (isMonitor(membro) && agora.isAfter(criacao.plusHours(24))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Monitores não podem alterar ou excluir ocorrências criadas há mais de 24 horas.");
        }
    }
}
