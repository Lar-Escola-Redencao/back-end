package br.org.larescolaredencao.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.AtualizarMembroDTO;
import br.org.larescolaredencao.dto.AtualizarPerfilDTO;
import br.org.larescolaredencao.dto.AlterarSenhaDTO;
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

    public Page<MembroResponseDTO> getAllMembros(Pageable pageable, Integer idPapel) {
        return getAllMembros(pageable, idPapel, null, null);
    }

    public Page<MembroResponseDTO> getAllMembros(Pageable pageable, Integer idPapel, String search) {
        return getAllMembros(pageable, idPapel, search, null);
    }

    public Page<MembroResponseDTO> getAllMembros(Pageable pageable, Integer idPapel, String search, Integer idUnidade) {
        String termo = TermoBusca.like(search);
        if (termo != null) {
            return membroRepository.buscar(termo, TermoBusca.digitosLike(search), idPapel, idUnidade, pageable)
                    .map(MembroResponseDTO::new);
        }
        Page<Membro> membros = (idPapel != null || idUnidade != null)
                ? membroRepository.filtrar(idPapel, idUnidade, pageable)
                : membroRepository.findAll(pageable);
        return membros.map(MembroResponseDTO::new);
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
        if (membroRepository.buscarPorCpf(dto.getCpf()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este CPF já está cadastrado no sistema.");
        }
        
        Papel papel = papelRepository.findById(dto.getIdPapel())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Papel não encontrado"));
        
        if (!"ADMINISTRADOR".equals(papel.getNomePapel()) && (dto.getIdsUnidades() == null || dto.getIdsUnidades().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione ao menos uma unidade para este perfil.");
        }

        List<Unidade> unidades = dto.getIdsUnidades() != null && !dto.getIdsUnidades().isEmpty() 
            ? unidadeService.buscarUnidadesPorIds(dto.getIdsUnidades()) 
            : List.of();
        
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

    @Transactional
    public MembroResponseDTO criarMembro(CriarMembroDTO dto, Membro membroLogado) {
        Papel papel = papelRepository.findById(dto.getIdPapel())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Papel não encontrado"));
        validarGestaoMembro(membroLogado, papel, dto.getIdsUnidades());
        return criarMembro(dto);
    }

    @Transactional
    public MembroResponseDTO atualizarMembro(Integer id, AtualizarMembroDTO dto, Membro membroLogado) {
        Membro membro = membroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
        validarGestaoMembro(membroLogado, membro.getPapel(),
                membro.getUnidades().stream().map(Unidade::getId).toList());
        Papel papel = papelRepository.findById(dto.getIdPapel())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Papel não encontrado"));
        validarGestaoMembro(membroLogado, papel, dto.getIdsUnidades());
        return atualizarMembro(id, dto);
    }

    @Transactional
    public void deletarMembro(Integer id, Membro membroLogado) {
        Membro membro = membroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
        validarGestaoMembro(membroLogado, membro.getPapel(),
                membro.getUnidades().stream().map(Unidade::getId).toList());
        deletarMembro(id);
    }

    private void validarGestaoMembro(Membro membroLogado, Papel papel, List<Integer> idsUnidades) {
        if ("ADMINISTRADOR".equals(membroLogado.getPapel().getNomePapel())) {
            return;
        }
        if (!"COORDENADOR".equals(membroLogado.getPapel().getNomePapel())
                || !"MONITOR".equals(papel.getNomePapel())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Coordenadores podem gerenciar somente monitores.");
        }
        if (idsUnidades != null && idsUnidades.stream().anyMatch(id -> membroLogado.getUnidades().stream()
                .noneMatch(unidade -> unidade.getId().equals(id)))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Não é permitido gerenciar monitores de outras unidades.");
        }
    }

    @Transactional
    public void alterarSenha(Integer idMembroLogado, AlterarSenhaDTO dto) {
        Membro membro = membroRepository.findById(idMembroLogado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
        validarSenhaAtual(membro, dto.getSenhaAtual());
        if (!dto.getNovaSenha().equals(dto.getConfirmacaoSenha())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A confirmação da senha não confere.");
        }
        membro.setSenha(passwordEncoder.encode(dto.getNovaSenha()));
        membroRepository.save(membro);
    }

    @Transactional(readOnly = true)
    public void validarSenhaAtual(Integer idMembroLogado, String senhaAtual) {
        Membro membro = membroRepository.findById(idMembroLogado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
        validarSenhaAtual(membro, senhaAtual);
    }

    private void validarSenhaAtual(Membro membro, String senhaAtual) {
        if (!passwordEncoder.matches(senhaAtual, membro.getSenha())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha atual está incorreta.");
        }
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

        String cpf = Membro.somenteDigitos(dto.getCpf());
        if (!Membro.somenteDigitos(membro.getCpf()).equals(cpf)) {
            Optional<Membro> membroComCpf = membroRepository.buscarPorCpf(cpf);
            if (membroComCpf.isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este CPF já está em uso por outro usuário.");
            }
        }

        Papel papel = papelRepository.findById(dto.getIdPapel())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Papel não encontrado"));
        
        if (!"ADMINISTRADOR".equals(papel.getNomePapel()) && (dto.getIdsUnidades() == null || dto.getIdsUnidades().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione ao menos uma unidade para este perfil.");
        }

        List<Unidade> unidades = dto.getIdsUnidades() != null && !dto.getIdsUnidades().isEmpty() 
            ? unidadeService.buscarUnidadesPorIds(dto.getIdsUnidades()) 
            : List.of();
        
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
    

    public MembroResponseDTO buscarPerfil(Integer idMembroLogado) {
        Membro membro = membroRepository.findById(idMembroLogado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
        return new MembroResponseDTO(membro);
    }
 
    public MembroResponseDTO atualizarPerfil(Integer idMembroLogado, AtualizarPerfilDTO dto) {
        Membro membro = membroRepository.findById(idMembroLogado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
 
        membro.setNomeCompleto(dto.getNomeCompleto());
        membro.setEndereco(dto.getEndereco());
        membro.setTelefone(dto.getTelefone());
 
        Membro salvo = membroRepository.save(membro);
        return new MembroResponseDTO(salvo);
    }

    public void deletarMembro(Integer id) {
        Membro membro = membroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
        membroRepository.delete(membro);
    }
}
