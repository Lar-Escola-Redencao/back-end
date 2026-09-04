package br.org.larescolaredencao.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.AtualizarMembroDTO;
import br.org.larescolaredencao.dto.CriarMembroDTO;
import br.org.larescolaredencao.dto.MembroResponseDTO;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Papel;
import br.org.larescolaredencao.repository.MembroRepository;
import br.org.larescolaredencao.repository.PapelRepository;
import br.org.larescolaredencao.model.Unidade;

@Service
public class MembroService {

    private final MembroRepository membroRepository;
    private final PapelRepository papelRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UnidadeService unidadeService;

    public MembroService(MembroRepository membroRepository, PapelRepository papelRepository, UnidadeService unidadeService) {
        this.membroRepository = membroRepository;
        this.papelRepository = papelRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.unidadeService = unidadeService;
    }

    public List<MembroResponseDTO> getAllMembros() {
        return membroRepository.findAll()
                .stream()
                .map(MembroResponseDTO::new)
                .collect(Collectors.toList());
    }

    public MembroResponseDTO getMembroById(Integer id) {
        Membro membro = membroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
        return new MembroResponseDTO(membro);
    }

    public MembroResponseDTO criarMembro(CriarMembroDTO dto) {
    	if (membroRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado no sistema.");
        }
        if (membroRepository.findByCpf(dto.getCpf()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este CPF já está cadastrado no sistema.");
        }
        
        Papel papel = papelRepository.findById(dto.getIdPapel())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Papel não encontrado"));

        List<Unidade> unidades = unidadeService.buscarUnidadesPorIds(dto.getIdsUnidades());
        
        Membro membro = new Membro();
        membro.setNomeCompleto(dto.getNomeCompleto());
        membro.setEmail(dto.getEmail());
        membro.setSenha(passwordEncoder.encode(dto.getSenha()));
        membro.setCpf(dto.getCpf());
        membro.setEndereco(dto.getEndereco());
        membro.setTelefone(dto.getTelefone());
        membro.setPapel(papel);
        membro.setUnidades(unidades);

        Membro salvo = membroRepository.save(membro);
        return new MembroResponseDTO(salvo);
    }

    public MembroResponseDTO atualizarMembro(Integer id, AtualizarMembroDTO dto) {
        Membro membro = membroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
        
        if (!membro.getEmail().equals(dto.getEmail())) {
            Optional<Membro> membroComEmail = membroRepository.findByEmail(dto.getEmail());
            if (membroComEmail.isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está em uso por outro usuário.");
            }
        }

        if (!membro.getCpf().equals(dto.getCpf())) {
            Optional<Membro> membroComCpf = membroRepository.findByCpf(dto.getCpf());
            if (membroComCpf.isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este CPF já está em uso por outro usuário.");
            }
        }

        Papel papel = papelRepository.findById(dto.getIdPapel())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Papel não encontrado"));
        
        List<Unidade> unidades = unidadeService.buscarUnidadesPorIds(dto.getIdsUnidades());
        
        membro.setNomeCompleto(dto.getNomeCompleto());
        membro.setEmail(dto.getEmail());
        membro.setCpf(dto.getCpf());
        membro.setEndereco(dto.getEndereco());
        membro.setTelefone(dto.getTelefone());
        membro.setPapel(papel);
        membro.setUnidades(unidades);

        Membro salvo = membroRepository.save(membro);
        return new MembroResponseDTO(salvo);
    }

    public void deletarMembro(Integer id) {
        Membro membro = membroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
        membroRepository.delete(membro);
    }
}