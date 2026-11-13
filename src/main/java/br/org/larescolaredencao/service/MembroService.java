package br.org.larescolaredencao.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
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
import br.org.larescolaredencao.dto.ReativarMembroDTO;
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
        return getAllMembros(pageable, idPapel, search, idUnidade, (Boolean) null);
    }

    private Page<MembroResponseDTO> getAllMembros(Pageable pageable, Integer idPapel, String search,
            Integer idUnidade, Boolean ativo) {
        String termo = TermoBusca.like(search);
        if (termo != null) {
            Page<Membro> encontrados = ativo == null
                    ? membroRepository.buscar(termo, TermoBusca.digitosLike(search), idPapel, idUnidade, pageable)
                    : membroRepository.buscarPorSituacao(termo, TermoBusca.digitosLike(search), idPapel, idUnidade,
                            ativo, pageable);
            return encontrados.map(MembroResponseDTO::new);
        }
        Page<Membro> membros = ativo != null
                ? membroRepository.filtrarPorSituacao(idPapel, idUnidade, ativo, pageable)
                : (idPapel != null || idUnidade != null)
                    ? membroRepository.filtrar(idPapel, idUnidade, pageable)
                    : membroRepository.findAll(pageable);
        return membros.map(MembroResponseDTO::new);
    }

    @Transactional(readOnly = true)
    public Page<MembroResponseDTO> getAllMembros(Pageable pageable, Integer idPapel, String search,
            Integer idUnidade, Membro membroLogado) {
        return getAllMembros(pageable, idPapel, search, idUnidade, null, membroLogado);
    }

    @Transactional(readOnly = true)
    public Page<MembroResponseDTO> getAllMembros(Pageable pageable, Integer idPapel, String search,
            Integer idUnidade, Boolean ativo, Membro membroLogado) {
        Membro solicitante = obterSolicitanteDaConsulta(membroLogado);
        if (pageable.getSort().equals(Sort.by(Sort.Direction.DESC, "ativo", "id"))) {
            boolean administrador = "ADMINISTRADOR".equals(solicitante.getPapel().getNomePapel());
            var idsUnidades = solicitante.getUnidades().stream().map(Unidade::getId).toList();
            if (!administrador && idUnidade != null && !idsUnidades.contains(idUnidade)) {
                return Page.empty(pageable);
            }
            return membroRepository.listarComProprioPrimeiro(solicitante.getId(), administrador, idsUnidades,
                    idPapel, idUnidade, TermoBusca.like(search), TermoBusca.digitosLike(search), ativo,
                    PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()))
                    .map(MembroResponseDTO::new);
        }
        if ("ADMINISTRADOR".equals(solicitante.getPapel().getNomePapel())) {
            return getAllMembros(pageable, idPapel, search, idUnidade, ativo);
        }
        List<Integer> permitidas = solicitante.getUnidades().stream().map(Unidade::getId).toList();
        if (permitidas.isEmpty() || (idUnidade != null && !permitidas.contains(idUnidade))) {
            return Page.empty(pageable);
        }
        Page<Membro> monitores = ativo == null
                ? membroRepository.listarMonitoresDasUnidades(permitidas, idPapel, idUnidade,
                        TermoBusca.like(search), TermoBusca.digitosLike(search), pageable)
                : membroRepository.listarMonitoresDasUnidadesPorSituacao(permitidas, idPapel, idUnidade,
                        TermoBusca.like(search), TermoBusca.digitosLike(search), ativo, pageable);
        return monitores.map(MembroResponseDTO::new);
    }

    @Transactional(readOnly = true)
    public MembroResponseDTO getMembroById(Integer id, Membro membroLogado) {
        Membro solicitante = obterSolicitanteDaConsulta(membroLogado);
        if ("ADMINISTRADOR".equals(solicitante.getPapel().getNomePapel())) return getMembroById(id);
        Membro membro = membroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
        var permitidas = solicitante.getUnidades().stream().map(Unidade::getId).collect(Collectors.toSet());
        if (!"MONITOR".equals(membro.getPapel().getNomePapel())
                || membro.getUnidades().stream().noneMatch(unidade -> permitidas.contains(unidade.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para consultar este colaborador.");
        }
        return new MembroResponseDTO(membro);
    }

    private Membro obterSolicitanteDaConsulta(Membro membroLogado) {
        if (membroLogado == null || membroLogado.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
        }
        Membro solicitante = membroRepository.findById(membroLogado.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Membro autenticado não encontrado."));
        String papel = solicitante.getPapel().getNomePapel();
        if (!"ADMINISTRADOR".equals(papel) && !"COORDENADOR".equals(papel)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para consultar colaboradores.");
        }
        return solicitante;
    }

    public MembroResponseDTO getMembroById(Integer id) {
        Membro membro = membroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
        return new MembroResponseDTO(membro);
    }

    @Transactional
    public MembroResponseDTO criarMembro(CriarMembroDTO dto, Membro membroLogado) {
        if (membroLogado == null || membroLogado.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
        }
        Membro solicitante = membroRepository.findById(membroLogado.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Membro autenticado não encontrado."));
        String perfil = solicitante.getPapel().getNomePapel();
        if (!"ADMINISTRADOR".equals(perfil) && !"COORDENADOR".equals(perfil)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para cadastrar colaboradores.");
        }
        Papel papel = papelRepository.findById(dto.getIdPapel())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Papel não encontrado"));
        if ("COORDENADOR".equals(perfil)) {
            if (!"MONITOR".equals(papel.getNomePapel())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Coordenadores só podem cadastrar monitores.");
            }
            var unidadesPermitidas = solicitante.getUnidades().stream()
                    .map(Unidade::getId).collect(Collectors.toSet());
            if (dto.getIdsUnidades() != null && !unidadesPermitidas.containsAll(dto.getIdsUnidades())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Selecione apenas unidades às quais você tem acesso.");
            }
        }
        membroRepository.findByEmail(dto.getEmail()).ifPresent(existente -> {
            throw conflitoDeDuplicidade(existente, "e-mail", "Este e-mail já está cadastrado no sistema.");
        });
        membroRepository.buscarPorCpf(dto.getCpf()).ifPresent(existente -> {
            throw conflitoDeDuplicidade(existente, "CPF", "Este CPF já está cadastrado no sistema.");
        });
        
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
    public MembroResponseDTO atualizarMembro(Integer id, AtualizarMembroDTO dto, Membro membroLogado) {
        validarGestaoDaPropriaConta(id, membroLogado);
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
        validarGestaoDaPropriaConta(id, membroLogado);
        Membro membro = membroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
        validarGestaoMembro(membroLogado, membro.getPapel(),
                membro.getUnidades().stream().map(Unidade::getId).toList());
        deletarMembro(id);
    }

    private void validarGestaoDaPropriaConta(Integer id, Membro membroLogado) {
        if (id.equals(membroLogado.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Não é permitido editar ou excluir a própria conta por aqui. Para alterar seus dados, acesse Meu perfil.");
        }
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
        aplicarDados(membro, dto);
        Membro salvo = membroRepository.save(membro);
        return new MembroResponseDTO(salvo);
    }

    /** Valida e aplica os dados cadastrais; não altera senha nem situação (ativo/inativo). */
    private void aplicarDados(Membro membro, AtualizarMembroDTO dto) {
        if (!membro.getEmail().equals(dto.getEmail())) {
            membroRepository.findByEmail(dto.getEmail()).ifPresent(outro -> {
                throw conflitoDeDuplicidade(outro, "e-mail", "Este e-mail já está em uso por outro usuário.");
            });
        }

        String cpf = Membro.somenteDigitos(dto.getCpf());
        if (!Membro.somenteDigitos(membro.getCpf()).equals(cpf)) {
            membroRepository.buscarPorCpf(cpf).ifPresent(outro -> {
                throw conflitoDeDuplicidade(outro, "CPF", "Este CPF já está em uso por outro usuário.");
            });
        }

        Papel papel = papelRepository.findById(dto.getIdPapel())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Papel não encontrado"));
        
        List<Unidade> unidades = dto.getIdsUnidades() == null
                ? membro.getUnidades()
                : dto.getIdsUnidades().isEmpty()
                    ? List.of()
                    : unidadeService.buscarUnidadesPorIds(dto.getIdsUnidades());

        if (!"ADMINISTRADOR".equals(papel.getNomePapel()) && unidades.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione ao menos uma unidade para este perfil.");
        }

        membro.setNomeCompleto(dto.getNomeCompleto());
        membro.setEmail(dto.getEmail());
        membro.setCpf(dto.getCpf());
        membro.setEndereco(dto.getEndereco());
        membro.setTelefone(dto.getTelefone());
        membro.setPapel(papel);
        var idsAtuais = membro.getUnidades().stream().map(Unidade::getId).collect(Collectors.toSet());
        var idsNovos = unidades.stream().map(Unidade::getId).collect(Collectors.toSet());
        if (!idsAtuais.equals(idsNovos)) {
            membro.getUnidades().clear();
            membro.getUnidades().addAll(unidades);
        }
    }

    private ResponseStatusException conflitoDeDuplicidade(Membro existente, String campo, String mensagemPadrao) {
        if (existente.isAtivo()) {
            return new ResponseStatusException(HttpStatus.CONFLICT, mensagemPadrao);
        }
        return new ResponseStatusException(HttpStatus.CONFLICT, "Este " + campo
                + " pertence a um colaborador inativo. Reative o cadastro existente em vez de criar outro.");
    }

    @Transactional
    public MembroResponseDTO desativarMembro(Integer id, Membro membroLogado) {
        if (id.equals(membroLogado.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não é possível desativar a própria conta.");
        }
        List<Membro> bloqueados = membroRepository.bloquearParaGestao(id);
        Membro membro = bloqueados.stream().filter(m -> id.equals(m.getId())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
        validarGestaoMembro(membroLogado, membro.getPapel(),
                membro.getUnidades().stream().map(Unidade::getId).toList());
        if (!membro.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este colaborador já está inativo.");
        }
        // A consulta trava o alvo e os administradores ativos: sem outro administrador ativo, não pode desativar.
        boolean ultimoAdministrador = "ADMINISTRADOR".equals(membro.getPapel().getNomePapel())
                && bloqueados.stream().noneMatch(m -> !id.equals(m.getId()));
        if (ultimoAdministrador) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível desativar o último administrador ativo.");
        }
        membro.setAtivo(false);
        membro.invalidarSessoes();
        return new MembroResponseDTO(membroRepository.save(membro));
    }

    @Transactional
    public MembroResponseDTO reativarMembro(Integer id, ReativarMembroDTO dto, Membro membroLogado) {
        validarGestaoDaPropriaConta(id, membroLogado);
        Membro membro = membroRepository.bloquearParaGestao(id).stream()
                .filter(m -> id.equals(m.getId())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
        validarGestaoMembro(membroLogado, membro.getPapel(),
                membro.getUnidades().stream().map(Unidade::getId).toList());
        if (membro.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este colaborador já está ativo.");
        }
        Papel papel = papelRepository.findById(dto.getIdPapel())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Papel não encontrado"));
        validarGestaoMembro(membroLogado, papel, dto.getIdsUnidades());
        if (dto.getConfirmacaoSenha() != null && !dto.getSenha().equals(dto.getConfirmacaoSenha())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A confirmação da senha não confere.");
        }
        if (passwordEncoder.matches(dto.getSenha(), membro.getSenha())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe uma nova senha, diferente da anterior.");
        }

        aplicarDados(membro, dto);
        membro.setSenha(passwordEncoder.encode(dto.getSenha()));
        membro.setAtivo(true);
        membro.invalidarSessoes();
        try {
            return new MembroResponseDTO(membroRepository.saveAndFlush(membro));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CPF ou e-mail já cadastrado em outro colaborador.");
        }
    }

    public MembroResponseDTO buscarPerfil(Integer idMembroLogado) {
        Membro membro = membroRepository.findById(idMembroLogado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
        return new MembroResponseDTO(membro);
    }
 
    @Transactional
    public MembroResponseDTO atualizarPerfil(Integer idMembroLogado, AtualizarPerfilDTO dto) {
        Membro membro = membroRepository.findById(idMembroLogado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));

        String email = dto.getEmail() == null ? membro.getEmail() : dto.getEmail().trim();
        String cpf = Membro.somenteDigitos(dto.getCpf() == null ? membro.getCpf() : dto.getCpf());
        boolean alteraEmail = email != null && !email.equalsIgnoreCase(membro.getEmail() == null ? null : membro.getEmail().trim());
        boolean alteraCpf = !java.util.Objects.equals(cpf, Membro.somenteDigitos(membro.getCpf()));
        if ((alteraEmail || alteraCpf) && !"ADMINISTRADOR".equals(membro.getPapel().getNomePapel())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente administradores podem alterar CPF e e-mail pelo Meu perfil.");
        }
        if (alteraEmail) {
            if (email.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um e-mail válido.");
            }
            membroRepository.findByEmail(email).filter(outro -> !outro.getId().equals(membro.getId()))
                    .ifPresent(outro -> { throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Este e-mail já está em uso por outro usuário."); });
        }
        if (alteraCpf) {
            if (cpf.length() != 11) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um CPF com 11 dígitos.");
            }
            membroRepository.buscarPorCpf(cpf).filter(outro -> !outro.getId().equals(membro.getId()))
                    .ifPresent(outro -> { throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Este CPF já está em uso por outro usuário."); });
        }

        if (alteraEmail) membro.setEmail(email);
        if (alteraCpf) membro.setCpf(cpf);
        membro.setNomeCompleto(dto.getNomeCompleto());
        membro.setEndereco(dto.getEndereco());
        membro.setTelefone(dto.getTelefone());
 
        Membro salvo = membroRepository.save(membro);
        return new MembroResponseDTO(salvo);
    }

    @Transactional
    public void deletarMembro(Integer id) {
        Membro membro = membroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
        try {
            membroRepository.delete(membro);
            membroRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível excluir este colaborador porque existem registros vinculados a ele, como entrevistas, presenças ou ocorrências. Esses registros precisam ser preservados.");
        }
    }
}
