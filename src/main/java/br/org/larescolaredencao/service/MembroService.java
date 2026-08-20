package br.org.larescolaredencao.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import br.org.larescolaredencao.dto.AtualizarMembroDTO;
import br.org.larescolaredencao.dto.CriarMembroDTO;
import br.org.larescolaredencao.dto.MembroResponseDTO;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Papel;
import br.org.larescolaredencao.repository.MembroRepository;
import br.org.larescolaredencao.repository.PapelRepository;

@Service
public class MembroService {

    private final MembroRepository membroRepository;
    private final PapelRepository papelRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public MembroService(MembroRepository membroRepository, PapelRepository papelRepository) {
        this.membroRepository = membroRepository;
        this.papelRepository = papelRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public List<MembroResponseDTO> getAllMembros() {
        return membroRepository.findAll()
                .stream()
                .map(MembroResponseDTO::new)
                .collect(Collectors.toList());
    }

    public MembroResponseDTO getMembroById(Integer id) {
        Membro membro = membroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Membro não encontrado"));
        return new MembroResponseDTO(membro);
    }

    public MembroResponseDTO criarMembro(CriarMembroDTO dto) {
    	if (membroRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Este e-mail já está cadastrado no sistema.");
        }
        if (membroRepository.findByCpf(dto.getCpf()).isPresent()) {
            throw new RuntimeException("Este CPF já está cadastrado no sistema.");
        }
        
        Papel papel = papelRepository.findById(dto.getIdPapel())
                .orElseThrow(() -> new RuntimeException("Papel não encontrado"));

        Membro membro = new Membro();
        membro.setNomeCompleto(dto.getNomeCompleto());
        membro.setEmail(dto.getEmail());
        membro.setSenha(passwordEncoder.encode(dto.getSenha()));
        membro.setCpf(dto.getCpf());
        membro.setEndereco(dto.getEndereco());
        membro.setTelefone(dto.getTelefone());
        membro.setPapel(papel);

        Membro salvo = membroRepository.save(membro);
        return new MembroResponseDTO(salvo);
    }

    public MembroResponseDTO atualizarMembro(Integer id, AtualizarMembroDTO dto) {
        Membro membro = membroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Membro não encontrado"));
        
        boolean nenhumaAlteracao = Objects.equals(membro.getNomeCompleto(), dto.getNomeCompleto()) &&
                Objects.equals(membro.getEmail(), dto.getEmail()) &&
                Objects.equals(membro.getCpf(), dto.getCpf()) &&
                Objects.equals(membro.getEndereco(), dto.getEndereco()) &&
                Objects.equals(membro.getTelefone(), dto.getTelefone()) &&
                Objects.equals(membro.getPapel().getId(), dto.getIdPapel());

        if (nenhumaAlteracao) {
            throw new RuntimeException("Nenhum dado foi alterado. A atualização não foi realizada.");
        }
        
        Optional<Membro> membroComEmail = membroRepository.findByEmail(dto.getEmail());
        if (membroComEmail.isPresent() && !membroComEmail.get().getId().equals(id)) {
            throw new RuntimeException("Este e-mail já está em uso por outro usuário.");
        }

        Optional<Membro> membroComCpf = membroRepository.findByCpf(dto.getCpf());
        if (membroComCpf.isPresent() && !membroComCpf.get().getId().equals(id)) {
            throw new RuntimeException("Este CPF já está em uso por outro usuário.");
        }

        Papel papel = papelRepository.findById(dto.getIdPapel())
                .orElseThrow(() -> new RuntimeException("Papel não encontrado"));

        membro.setNomeCompleto(dto.getNomeCompleto());
        membro.setEmail(dto.getEmail());
        membro.setCpf(dto.getCpf());
        membro.setEndereco(dto.getEndereco());
        membro.setTelefone(dto.getTelefone());
        membro.setPapel(papel);

        Membro salvo = membroRepository.save(membro);
        return new MembroResponseDTO(salvo);
    }

    public void deletarMembro(Integer id) {
        Membro membro = membroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Membro não encontrado"));
        membroRepository.delete(membro);
    }
}