package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.AssistidoResponseDTO;
import br.org.larescolaredencao.dto.AtualizarVinculoDTO;
import br.org.larescolaredencao.dto.ContatoDTO;
import br.org.larescolaredencao.dto.CriarAssistidoDTO;
import br.org.larescolaredencao.dto.TransferirTurmaDTO;
import br.org.larescolaredencao.dto.VincularContatoExistenteDTO;
import br.org.larescolaredencao.model.Assistido;
import br.org.larescolaredencao.model.Contato;
import br.org.larescolaredencao.model.ContatoAssistido;
import br.org.larescolaredencao.model.ContatoAssistidoId;
import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.Turma;
import br.org.larescolaredencao.model.enums.StatusMatricula;
import br.org.larescolaredencao.repository.AssistidoRepository;
import br.org.larescolaredencao.repository.ContatoAssistidoRepository;
import br.org.larescolaredencao.repository.ContatoRepository;
import br.org.larescolaredencao.repository.MatriculaRepository;
import br.org.larescolaredencao.repository.TurmaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssistidoService {

    private final AssistidoRepository assistidoRepository;
    private final MatriculaRepository matriculaRepository;
    private final ContatoRepository contatoRepository;
    private final ContatoAssistidoRepository contatoAssistidoRepository;
    private final TurmaRepository turmaRepository;
    private final ArquivoService arquivoService;

    public AssistidoService(AssistidoRepository assistidoRepository,
                            MatriculaRepository matriculaRepository,
                            ContatoRepository contatoRepository,
                            ContatoAssistidoRepository contatoAssistidoRepository,
                            TurmaRepository turmaRepository,
                            ArquivoService arquivoService) {
        this.assistidoRepository = assistidoRepository;
        this.matriculaRepository = matriculaRepository;
        this.contatoRepository = contatoRepository;
        this.contatoAssistidoRepository = contatoAssistidoRepository;
        this.turmaRepository = turmaRepository;
        this.arquivoService = arquivoService;
    }

    @Transactional(readOnly = true)
    public List<AssistidoResponseDTO> listarAssistidosDoMembro(Integer membroId) {
        return assistidoRepository.findAtivosByMembroId(membroId)
                .stream()
                .map(AssistidoResponseDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AssistidoResponseDTO buscarAssistidoPorId(Integer id) {
        Assistido assistido = assistidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assistido não encontrado."));
        
        List<ContatoAssistido> contatos = contatoAssistidoRepository.findByAssistido(assistido);
        
        return new AssistidoResponseDTO(assistido, contatos);
    }

    @Transactional
    public AssistidoResponseDTO cadastrarAssistido(CriarAssistidoDTO dto) {
        Assistido assistido = null;

        if (dto.getCpf() != null && !dto.getCpf().isBlank()) {
            assistido = assistidoRepository.findByCpf(dto.getCpf()).orElse(null);
        }

        if (assistido == null && dto.getDocumentoAuxiliar() != null && !dto.getDocumentoAuxiliar().isBlank() && dto.getTipoDocumento() != null) {
            assistido = assistidoRepository.findFirstByDocumentoAuxiliarAndTipoDocumento(dto.getDocumentoAuxiliar(), dto.getTipoDocumento()).orElse(null);
        }

        if (assistido != null) {
            List<Matricula> matriculas = matriculaRepository.findByAssistido(assistido);
            boolean possuiAtiva = matriculas.stream()
                    .anyMatch(m -> m.getStatus() == StatusMatricula.ATIVO);

            if (possuiAtiva) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Assistido já possui matrícula ATIVA.");
            }

            assistido.setNomeCompleto(dto.getNomeCompleto());
            assistido.setDataNascimento(dto.getDataNascimento());
            if (dto.getCpf() != null && !dto.getCpf().isBlank()) {
                assistido.setCpf(dto.getCpf());
            }
            if (dto.getDocumentoAuxiliar() != null && !dto.getDocumentoAuxiliar().isBlank()) {
                assistido.setDocumentoAuxiliar(dto.getDocumentoAuxiliar());
                assistido.setTipoDocumento(dto.getTipoDocumento());
            }
            assistido.setEndereco(dto.getEndereco());
        } else {
            assistido = novoAssistido(dto);
        }

        Assistido salvo = assistidoRepository.save(assistido);

        if (dto.getContatos() != null) {
            for (ContatoDTO contatoDTO : dto.getContatos()) {
                Contato contato;
                
                if (contatoDTO.getId() != null) {
                    contato = contatoRepository.findById(contatoDTO.getId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contato vinculado por ID não encontrado."));
                    
                    contato.setNomeCompleto(contatoDTO.getNomeCompleto());
                    contato.setTelefone(contatoDTO.getTelefone());
                    if (contatoDTO.getEmail() != null) contato.setEmail(contatoDTO.getEmail());
                    if (contatoDTO.getEndereco() != null) contato.setEndereco(contatoDTO.getEndereco());
                    contato = contatoRepository.save(contato);
                } else {
                    contato = contatoRepository.findByTelefone(contatoDTO.getTelefone())
                            .map(c -> {
                                c.setNomeCompleto(contatoDTO.getNomeCompleto());
                                if (contatoDTO.getEmail() != null) c.setEmail(contatoDTO.getEmail());
                                if (contatoDTO.getEndereco() != null) c.setEndereco(contatoDTO.getEndereco());
                                return contatoRepository.save(c);
                            })
                            .orElseGet(() -> {
                                Contato c = new Contato();
                                c.setNomeCompleto(contatoDTO.getNomeCompleto());
                                c.setTelefone(contatoDTO.getTelefone());
                                c.setEmail(contatoDTO.getEmail());
                                c.setEndereco(contatoDTO.getEndereco());
                                return contatoRepository.save(c);
                            });
                }

                ContatoAssistidoId caId = new ContatoAssistidoId(salvo.getId(), contato.getId());
                if (contatoAssistidoRepository.existsById(caId)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Este contato já está vinculado ao assistido.");
                }

                if (contatoDTO.getPrincipal()) {
                    removerPrincipalAtual(salvo);
                }

                ContatoAssistido ca = new ContatoAssistido();
                ca.setId(caId);
                ca.setAssistido(salvo);
                ca.setContato(contato);
                ca.setParentesco(contatoDTO.getParentesco());
                ca.setPrincipal(contatoDTO.getPrincipal());
                contatoAssistidoRepository.save(ca);
            }
        }

        Turma turma = turmaRepository.findById(dto.getIdTurma())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada."));

        Matricula matricula = new Matricula();
        matricula.setAssistido(salvo);
        matricula.setTurma(turma);
        matricula.setStatus(StatusMatricula.ATIVO);
        matricula.setDataIngresso(LocalDate.now());
        matriculaRepository.save(matricula);

        List<ContatoAssistido> contatosSalvos = contatoAssistidoRepository.findByAssistido(salvo);
        return new AssistidoResponseDTO(salvo, contatosSalvos);
    }

    @Transactional
    public void atualizarFotoPerfil(Integer id, MultipartFile foto) {
        Assistido assistido = assistidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assistido não encontrado."));

        arquivoService.validarTipoArquivo(foto, TipoArquivo.FOTO);
        
        if (assistido.getImagemPerfil() != null) {
            arquivoService.deletarArquivo(assistido.getImagemPerfil());
        }

        String caminhoFoto = arquivoService.salvarArquivo(foto, "assistidos/", TipoArquivo.FOTO);
        assistido.setImagemPerfil(caminhoFoto);
        assistidoRepository.save(assistido);
    }

    @Transactional
    public AssistidoResponseDTO transferirTurma(Integer assistidoId, TransferirTurmaDTO dto) {
        Assistido assistido = assistidoRepository.findById(assistidoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assistido não encontrado."));

        List<Matricula> matriculas = matriculaRepository.findByAssistido(assistido);
        Matricula matriculaAtiva = matriculas.stream()
                .filter(m -> m.getStatus() == StatusMatricula.ATIVO)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "O assistido não possui matrícula ativa para transferir."));

        if (matriculaAtiva.getTurma().getId().equals(dto.getIdTurmaNova())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O assistido já está matriculado nesta turma.");
        }

        Turma novaTurma = turmaRepository.findById(dto.getIdTurmaNova())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nova turma não encontrada."));

        long dias = ChronoUnit.DAYS.between(matriculaAtiva.getDataIngresso(), LocalDate.now());

        if (dias <= 1) {
            matriculaAtiva.setTurma(novaTurma);
            matriculaRepository.save(matriculaAtiva);
        } else {
            matriculaAtiva.setStatus(StatusMatricula.INATIVO);
            matriculaAtiva.setDataDesligamento(LocalDate.now());
            matriculaRepository.save(matriculaAtiva);

            Matricula novaMatricula = new Matricula();
            novaMatricula.setAssistido(assistido);
            novaMatricula.setTurma(novaTurma);
            novaMatricula.setStatus(StatusMatricula.ATIVO);
            novaMatricula.setDataIngresso(LocalDate.now());
            matriculaRepository.save(novaMatricula);
        }

        List<ContatoAssistido> contatos = contatoAssistidoRepository.findByAssistido(assistido);
        return new AssistidoResponseDTO(assistido, contatos);
    }

    @Transactional
    public AssistidoResponseDTO vincularContatoExistente(Integer assistidoId, Integer contatoId, VincularContatoExistenteDTO dto) {
        Assistido assistido = assistidoRepository.findById(assistidoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assistido não encontrado."));

        Contato contato = contatoRepository.findById(contatoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contato não encontrado."));

        validarLimiteVinculos(assistido);

        ContatoAssistidoId caId = new ContatoAssistidoId(assistido.getId(), contato.getId());
        if (contatoAssistidoRepository.existsById(caId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este contato já está vinculado ao assistido.");
        }

        if (dto.getPrincipal()) {
            removerPrincipalAtual(assistido);
        }

        ContatoAssistido ca = new ContatoAssistido();
        ca.setId(caId);
        ca.setAssistido(assistido);
        ca.setContato(contato);
        ca.setParentesco(dto.getParentesco());
        ca.setPrincipal(dto.getPrincipal());
        contatoAssistidoRepository.save(ca);

        return buscarAssistidoPorId(assistidoId);
    }

    @Transactional
    public AssistidoResponseDTO vincularNovoContato(Integer assistidoId, ContatoDTO dto) {
        Assistido assistido = assistidoRepository.findById(assistidoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assistido não encontrado."));

        validarLimiteVinculos(assistido);

        Contato contato;
        
        if (dto.getId() != null) {
            contato = contatoRepository.findById(dto.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contato vinculado por ID não encontrado."));
            
            contato.setNomeCompleto(dto.getNomeCompleto());
            contato.setTelefone(dto.getTelefone());
            if (dto.getEmail() != null) contato.setEmail(dto.getEmail());
            if (dto.getEndereco() != null) contato.setEndereco(dto.getEndereco());
            contato = contatoRepository.save(contato);
        } else {
            contato = contatoRepository.findByTelefone(dto.getTelefone())
                    .map(c -> {
                        c.setNomeCompleto(dto.getNomeCompleto());
                        if (dto.getEmail() != null) c.setEmail(dto.getEmail());
                        if (dto.getEndereco() != null) c.setEndereco(dto.getEndereco());
                        return contatoRepository.save(c);
                    })
                    .orElseGet(() -> {
                        Contato c = new Contato();
                        c.setNomeCompleto(dto.getNomeCompleto());
                        c.setTelefone(dto.getTelefone());
                        c.setEmail(dto.getEmail());
                        c.setEndereco(dto.getEndereco());
                        return contatoRepository.save(c);
                    });
        }

        ContatoAssistidoId caId = new ContatoAssistidoId(assistido.getId(), contato.getId());
        if (contatoAssistidoRepository.existsById(caId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este contato já está vinculado ao assistido.");
        }

        if (dto.getPrincipal()) {
            removerPrincipalAtual(assistido);
        }

        ContatoAssistido ca = new ContatoAssistido();
        ca.setId(caId);
        ca.setAssistido(assistido);
        ca.setContato(contato);
        ca.setParentesco(dto.getParentesco());
        ca.setPrincipal(dto.getPrincipal());
        contatoAssistidoRepository.save(ca);

        return buscarAssistidoPorId(assistidoId);
    }

    @Transactional
    public void atualizarVinculo(Integer assistidoId, Integer contatoId, AtualizarVinculoDTO dto) {
        ContatoAssistido vinculo = contatoAssistidoRepository.findByAssistidoIdAndContatoId(assistidoId, contatoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo não encontrado."));

        if (dto.getPrincipal() && !vinculo.getPrincipal()) {
            removerPrincipalAtual(vinculo.getAssistido());
        } else if (!dto.getPrincipal() && vinculo.getPrincipal()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível remover o status de principal diretamente. Marque outro contato como principal para substituí-lo.");
        }

        vinculo.setParentesco(dto.getParentesco());
        vinculo.setPrincipal(dto.getPrincipal());
        contatoAssistidoRepository.save(vinculo);
    }

    @Transactional
    public void desvincularContato(Integer assistidoId, Integer contatoId) {
        ContatoAssistido vinculo = contatoAssistidoRepository.findByAssistidoIdAndContatoId(assistidoId, contatoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo não encontrado."));

        if (vinculo.getPrincipal()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível desvincular o contato principal. Marque outro contato como principal antes.");
        }

        contatoAssistidoRepository.delete(vinculo);
    }

    @Transactional
    public void deletarAssistido(Integer id) {
        Assistido assistido = assistidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assistido não encontrado."));

        if (assistido.getImagemPerfil() != null) {
            arquivoService.deletarArquivo(assistido.getImagemPerfil());
        }

        assistidoRepository.delete(assistido);
    }

    private void validarLimiteVinculos(Assistido assistido) {
        long totalVinculos = contatoAssistidoRepository.countByAssistido(assistido);
        if (totalVinculos >= 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O assistido já atingiu o limite máximo de 4 responsáveis.");
        }
    }

    private void removerPrincipalAtual(Assistido assistido) {
        contatoAssistidoRepository.findByAssistidoAndPrincipalTrue(assistido)
                .ifPresent(antigo -> {
                    antigo.setPrincipal(false);
                    contatoAssistidoRepository.save(antigo);
                });
    }

    private Assistido novoAssistido(CriarAssistidoDTO dto) {
        Assistido assistido = new Assistido();
        assistido.setNomeCompleto(dto.getNomeCompleto());
        assistido.setDataNascimento(dto.getDataNascimento());
        assistido.setCpf(dto.getCpf());
        assistido.setDocumentoAuxiliar(dto.getDocumentoAuxiliar());
        assistido.setTipoDocumento(dto.getTipoDocumento());
        assistido.setEndereco(dto.getEndereco());
        return assistido;
    }
}