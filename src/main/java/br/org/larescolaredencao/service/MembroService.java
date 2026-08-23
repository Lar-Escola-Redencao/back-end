package br.org.larescolaredencao.service;

import java.util.List;
import java.util.Objects;
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
        
        boolean alterouAlgo = false;

        if (dto.getNomeCompleto() != null && !dto.getNomeCompleto().trim().isEmpty() && !isTextoIgual(membro.getNomeCompleto(), dto.getNomeCompleto())) {
            membro.setNomeCompleto(dto.getNomeCompleto());
            alterouAlgo = true;
        }

        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty() && !isTextoIgual(membro.getEmail(), dto.getEmail())) {
            Optional<Membro> membroComEmail = membroRepository.findByEmail(dto.getEmail());
            if (membroComEmail.isPresent() && !membroComEmail.get().getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está em uso por outro usuário.");
            }
            membro.setEmail(dto.getEmail());
            alterouAlgo = true;
        }

        if (dto.getCpf() != null && !dto.getCpf().trim().isEmpty() && !isTextoIgual(membro.getCpf(), dto.getCpf())) {
            Optional<Membro> membroComCpf = membroRepository.findByCpf(dto.getCpf());
            if (membroComCpf.isPresent() && !membroComCpf.get().getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este CPF já está em uso por outro usuário.");
            }
            membro.setCpf(dto.getCpf());
            alterouAlgo = true;
        }

        if (dto.getEndereco() != null && !isTextoIgual(membro.getEndereco(), dto.getEndereco())) {
            membro.setEndereco(dto.getEndereco());
            alterouAlgo = true;
        }

        if (dto.getTelefone() != null && !isTextoIgual(membro.getTelefone(), dto.getTelefone())) {
            membro.setTelefone(dto.getTelefone());
            alterouAlgo = true;
        }

        if (dto.getIdPapel() != null && !dto.getIdPapel().equals(membro.getPapel().getId())) {
            Papel papel = papelRepository.findById(dto.getIdPapel())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Papel não encontrado"));
            membro.setPapel(papel);
            alterouAlgo = true;
        }

        if (!alterouAlgo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhum dado foi alterado. A atualização não foi realizada.");
        }

        Membro salvo = membroRepository.save(membro);
        return new MembroResponseDTO(salvo);
    }

    public void deletarMembro(Integer id) {
        Membro membro = membroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
        membroRepository.delete(membro);
    }
    
    private boolean isTextoIgual(String str1, String str2) {
        String s1 = (str1 == null) ? "" : str1.trim();
        String s2 = (str2 == null) ? "" : str2.trim();
        return s1.equals(s2);
    }
}