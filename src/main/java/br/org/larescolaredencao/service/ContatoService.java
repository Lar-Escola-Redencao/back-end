package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.AtualizarContatoDTO;
import br.org.larescolaredencao.dto.ContatoListagemDTO;
import br.org.larescolaredencao.dto.VinculoContatoResponseDTO;
import br.org.larescolaredencao.model.Contato;
import br.org.larescolaredencao.repository.ContatoAssistidoRepository;
import br.org.larescolaredencao.repository.ContatoRepository;
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

    public ContatoService(ContatoRepository contatoRepository, ContatoAssistidoRepository contatoAssistidoRepository) {
        this.contatoRepository = contatoRepository;
        this.contatoAssistidoRepository = contatoAssistidoRepository;
    }

    @Transactional(readOnly = true)
    public Page<ContatoListagemDTO> listarContatos(Integer membroId, Pageable pageable) {
        return contatoRepository.findVisibleByMembroId(membroId, pageable).map(contato -> {
            long vinculos = contatoAssistidoRepository.countByContatoId(contato.getId());
            return new ContatoListagemDTO(contato, vinculos);
        });
    }

    @Transactional(readOnly = true)
    public List<ContatoListagemDTO> buscarContatosAutocomplete(Integer membroId, String termo) {
        return contatoRepository.searchVisibleByMembroIdAndTermo(membroId, termo).stream()
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
                .map(VinculoContatoResponseDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public ContatoListagemDTO criarContatoAvulso(AtualizarContatoDTO dto) {
        Contato contato = contatoRepository.findByTelefone(dto.getTelefone())
                .map(c -> {
                    c.setNomeCompleto(dto.getNomeCompleto());
                    c.setEmail(dto.getEmail());
                    c.setEndereco(dto.getEndereco());
                    return c;
                })
                .orElseGet(() -> {
                    Contato c = new Contato();
                    c.setNomeCompleto(dto.getNomeCompleto());
                    c.setTelefone(dto.getTelefone());
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

        if (!contato.getTelefone().equals(dto.getTelefone()) && contatoRepository.findByTelefone(dto.getTelefone()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe outro contato cadastrado com este telefone.");
        }

        contato.setNomeCompleto(dto.getNomeCompleto());
        contato.setTelefone(dto.getTelefone());
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