package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.AtualizarContatoDTO;
import br.org.larescolaredencao.dto.ContatoListagemDTO;
import br.org.larescolaredencao.dto.VinculoContatoResponseDTO;
import br.org.larescolaredencao.model.Assistido;
import br.org.larescolaredencao.model.Contato;
import br.org.larescolaredencao.model.ContatoAssistido;
import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.enums.StatusMatricula;
import br.org.larescolaredencao.repository.ContatoAssistidoRepository;
import br.org.larescolaredencao.repository.ContatoRepository;
import br.org.larescolaredencao.repository.MatriculaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContatoService {

    private final ContatoRepository contatoRepository;
    private final ContatoAssistidoRepository contatoAssistidoRepository;
    private final MatriculaRepository matriculaRepository;

    public ContatoService(ContatoRepository contatoRepository, ContatoAssistidoRepository contatoAssistidoRepository, MatriculaRepository matriculaRepository) {
        this.contatoRepository = contatoRepository;
        this.contatoAssistidoRepository = contatoAssistidoRepository;
        this.matriculaRepository = matriculaRepository;
    }

    @Transactional(readOnly = true)
    public Page<ContatoListagemDTO> listarContatos(Integer membroId, Pageable pageable) {
        return contatoRepository.findVisibleByMembroId(membroId, pageable).map(contato -> {
            long vinculos = contatoAssistidoRepository.countByContatoId(contato.getId());
            return new ContatoListagemDTO(contato, vinculos);
        });
    }

    @Transactional(readOnly = true)
    public ContatoListagemDTO buscarContato(Integer id) {
        Contato contato = contatoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contato não encontrado."));

        List<ContatoAssistido> contatosAssistidos = contatoAssistidoRepository.findByContatoId(contato.getId());

        List<VinculoContatoResponseDTO> vinculos = contatosAssistidos.stream().map(ca -> {
            Assistido assistido = ca.getAssistido();
            Matricula matriculaAtiva = matriculaRepository.findByAssistido(assistido).stream()
                    .filter(m -> m.getStatus() == StatusMatricula.ATIVO)
                    .findFirst()
                    .orElse(null);
            
            return new VinculoContatoResponseDTO(ca, matriculaAtiva);
        }).collect(Collectors.toList());

        ContatoListagemDTO dto = new ContatoListagemDTO(contato, vinculos.size());
        dto.setVinculos(vinculos);
        
        return dto;
    }

    @Transactional(readOnly = true)
    public List<ContatoListagemDTO> buscarContatosAutocomplete(Integer membroId, String termo) {
        String termoTelefone = termo.replaceAll("\\D", "");
        if (termoTelefone.isEmpty()) {
            termoTelefone = null;
        }

        return contatoRepository.searchVisibleByMembroIdAndTermo(membroId, termo, termoTelefone).stream()
                .map(contato -> {
                    long vinculos = contatoAssistidoRepository.countByContatoId(contato.getId());
                    return new ContatoListagemDTO(contato, vinculos);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VinculoContatoResponseDTO> listarVinculosDoContato(Integer contatoId) {
        if (!contatoRepository.existsById(contatoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contato não encontrado.");
        }
        
        return contatoAssistidoRepository.findByContatoId(contatoId)
                .stream()
                .map(ca -> {
                    Matricula matriculaAtiva = matriculaRepository.findByAssistido(ca.getAssistido()).stream()
                            .filter(m -> m.getStatus() == StatusMatricula.ATIVO)
                            .findFirst()
                            .orElse(null);
                    return new VinculoContatoResponseDTO(ca, matriculaAtiva);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ContatoListagemDTO criarContatoAvulso(AtualizarContatoDTO dto) {
        String telefoneLimpo = dto.getTelefone() != null ? dto.getTelefone().replaceAll("\\D", "") : null;

        Contato contato = contatoRepository.findByTelefone(telefoneLimpo)
                .map(c -> {
                    c.setNomeCompleto(dto.getNomeCompleto());
                    c.setEmail(dto.getEmail());
                    c.setEndereco(dto.getEndereco());
                    return c;
                })
                .orElseGet(() -> {
                    Contato c = new Contato();
                    c.setNomeCompleto(dto.getNomeCompleto());
                    c.setTelefone(telefoneLimpo);
                    c.setEmail(dto.getEmail());
                    c.setEndereco(dto.getEndereco());
                    return c;
                });
        
        Contato salvo = contatoRepository.save(contato);
        long vinculos = contatoAssistidoRepository.countByContatoId(salvo.getId());
        return new ContatoListagemDTO(salvo, vinculos);
    }

    @Transactional
    public ContatoListagemDTO atualizarContato(Integer id, AtualizarContatoDTO dto) {
        Contato contato = contatoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contato não encontrado."));

        String telefoneLimpo = dto.getTelefone() != null ? dto.getTelefone().replaceAll("\\D", "") : null;

        if (!contato.getTelefone().equals(telefoneLimpo) && contatoRepository.findByTelefone(telefoneLimpo).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe outro contato cadastrado com este telefone.");
        }

        contato.setNomeCompleto(dto.getNomeCompleto());
        contato.setTelefone(telefoneLimpo);
        contato.setEmail(dto.getEmail());
        contato.setEndereco(dto.getEndereco());

        Contato salvo = contatoRepository.save(contato);
        long vinculos = contatoAssistidoRepository.countByContatoId(salvo.getId());

        return new ContatoListagemDTO(salvo, vinculos);
    }

    @Transactional
    public void deletarContato(Integer id) {
        Contato contato = contatoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contato não encontrado."));

        long vinculos = contatoAssistidoRepository.countByContatoId(id);
        if (vinculos > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível excluir o contato pois ele possui " + vinculos + " vínculo(s) ativo(s). Desvincule-o dos assistidos primeiro.");
        }

        contatoRepository.delete(contato);
    }
}