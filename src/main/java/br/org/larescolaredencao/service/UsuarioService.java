package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.AtualizarUsuarioDTO;
import br.org.larescolaredencao.dto.AtualizarVinculoDTO;
import br.org.larescolaredencao.dto.CadastroUsuarioCompletoDTO;
import br.org.larescolaredencao.dto.ComposicaoFamiliarDTO;
import br.org.larescolaredencao.dto.ContatoDTO;
import br.org.larescolaredencao.dto.FichaSocioeconomicaDTO;
import br.org.larescolaredencao.dto.InativarUsuarioDTO;
import br.org.larescolaredencao.dto.TransferirTurmaDTO;
import br.org.larescolaredencao.dto.UsuarioResponseDTO;
import br.org.larescolaredencao.dto.VincularContatoExistenteDTO;
import br.org.larescolaredencao.model.ArquivoSaude;
import br.org.larescolaredencao.model.ComposicaoFamiliar;
import br.org.larescolaredencao.model.Contato;
import br.org.larescolaredencao.model.ContatoUsuario;
import br.org.larescolaredencao.model.ContatoUsuarioId;
import br.org.larescolaredencao.model.FichaSocioeconomica;
import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Turma;
import br.org.larescolaredencao.model.Usuario;
import br.org.larescolaredencao.model.enums.Perfil;
import br.org.larescolaredencao.model.enums.StatusMatricula;
import br.org.larescolaredencao.repository.ArquivoSaudeRepository;
import br.org.larescolaredencao.repository.ComposicaoFamiliarRepository;
import br.org.larescolaredencao.repository.ContatoRepository;
import br.org.larescolaredencao.repository.ContatoUsuarioRepository;
import br.org.larescolaredencao.repository.EntrevistaSocialRepository;
import br.org.larescolaredencao.repository.FichaSocioeconomicaRepository;
import br.org.larescolaredencao.repository.MatriculaRepository;
import br.org.larescolaredencao.repository.MembroRepository;
import br.org.larescolaredencao.repository.TurmaRepository;
import br.org.larescolaredencao.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final MatriculaRepository matriculaRepository;
    private final ContatoRepository contatoRepository;
    private final ContatoUsuarioRepository contatoUsuarioRepository;
    private final TurmaRepository turmaRepository;
    private final ArquivoService arquivoService;
    private final FichaSocioeconomicaRepository fichaSocioeconomicaRepository;
    private final ComposicaoFamiliarRepository composicaoFamiliarRepository;
    private final ArquivoSaudeRepository arquivoSaudeRepository;
    private final EntrevistaSocialRepository entrevistaSocialRepository;
    private final MembroRepository membroRepository;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          MatriculaRepository matriculaRepository,
                          ContatoRepository contatoRepository,
                          ContatoUsuarioRepository contatoUsuarioRepository,
                          TurmaRepository turmaRepository,
                          ArquivoService arquivoService,
                          FichaSocioeconomicaRepository fichaSocioeconomicaRepository,
                          ComposicaoFamiliarRepository composicaoFamiliarRepository,
                          ArquivoSaudeRepository arquivoSaudeRepository,
                          EntrevistaSocialRepository entrevistaSocialRepository,
                          MembroRepository membroRepository) {
        this.usuarioRepository = usuarioRepository;
        this.matriculaRepository = matriculaRepository;
        this.contatoRepository = contatoRepository;
        this.contatoUsuarioRepository = contatoUsuarioRepository;
        this.turmaRepository = turmaRepository;
        this.arquivoService = arquivoService;
        this.fichaSocioeconomicaRepository = fichaSocioeconomicaRepository;
        this.composicaoFamiliarRepository = composicaoFamiliarRepository;
        this.arquivoSaudeRepository = arquivoSaudeRepository;
        this.entrevistaSocialRepository = entrevistaSocialRepository;
        this.membroRepository = membroRepository;
    }

    private Matricula obterMatriculaAtiva(Usuario usuario) {
        return matriculaRepository.findByUsuario(usuario).stream()
                .filter(m -> m.getStatus() == StatusMatricula.ATIVO)
                .findFirst()
                .orElse(null);
    }

    private void validarAcessoAoUsuario(Usuario usuario, Membro membroLogado) {
        if (Perfil.ADMINISTRADOR.name().equals(membroLogado.getPapel().getNomePapel())) {
            return;
        }
        Matricula matriculaAtiva = obterMatriculaAtiva(usuario);
        if (matriculaAtiva != null) {
            boolean hasAccess = membroLogado.getUnidades().stream()
                    .anyMatch(u -> u.getId().equals(matriculaAtiva.getTurma().getUnidade().getId()));
            if (!hasAccess) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: Você não gerencia a unidade atual deste usuário.");
            }
        }
    }

    private void validarAcessoATurma(Integer idTurma, Membro membroLogado) {
        if (Perfil.ADMINISTRADOR.name().equals(membroLogado.getPapel().getNomePapel())) {
            return;
        }
        Turma turma = turmaRepository.findById(idTurma)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada."));
        
        boolean hasAccess = membroLogado.getUnidades().stream()
                .anyMatch(u -> u.getId().equals(turma.getUnidade().getId()));
        if (!hasAccess) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: Você não tem permissão para cadastrar alunos nesta unidade.");
        }
    }


    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarUsuariosDoMembro(Integer membroId) {
        Membro membro = membroRepository.findById(membroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado."));

        List<Usuario> usuarios;
        if (Perfil.ADMINISTRADOR.name().equals(membro.getPapel().getNomePapel())) {
            usuarios = usuarioRepository.findAtivos();
        } else {
            usuarios = usuarioRepository.findAtivosByMembroId(membroId);
        }

        return usuarios.stream()
                .map(u -> new UsuarioResponseDTO(u, null, obterMatriculaAtiva(u)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarUsuarioPorId(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        List<ContatoUsuario> contatos = contatoUsuarioRepository.findByUsuario(usuario);

        return new UsuarioResponseDTO(usuario, contatos, obterMatriculaAtiva(usuario));
    }

    @Transactional
    public UsuarioResponseDTO cadastrarUsuario(CadastroUsuarioCompletoDTO dto, Membro membroLogado) {
        validarAcessoATurma(dto.getIdTurma(), membroLogado);

        Usuario usuario = null;
        boolean reingresso = false;

        if (dto.getCpf() != null && !dto.getCpf().isBlank()) {
            usuario = usuarioRepository.findByCpf(dto.getCpf()).orElse(null);
        }

        if (usuario == null && dto.getDocumentoAuxiliar() != null && !dto.getDocumentoAuxiliar().isBlank() && dto.getTipoDocumento() != null) {
            usuario = usuarioRepository.findFirstByDocumentoAuxiliarAndTipoDocumento(dto.getDocumentoAuxiliar(), dto.getTipoDocumento()).orElse(null);
        }

        if (usuario != null) {
            List<Matricula> matriculas = matriculaRepository.findByUsuario(usuario);
            boolean possuiAtiva = matriculas.stream()
                    .anyMatch(m -> m.getStatus() == StatusMatricula.ATIVO);

            if (possuiAtiva) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Usuário já possui matrícula ATIVA.");
            }

            reingresso = true;
            mapearDadosUsuario(usuario, dto);
        } else {
            usuario = novoUsuario(dto);
        }

        Usuario salvo = usuarioRepository.save(usuario);

        if (reingresso) {
            List<ContatoUsuario> vinculosAntigos = contatoUsuarioRepository.findByUsuario(salvo);
            if (!vinculosAntigos.isEmpty()) {
                contatoUsuarioRepository.deleteAll(vinculosAntigos);
            }
            composicaoFamiliarRepository.deleteByIdUsuario(salvo.getId());
            fichaSocioeconomicaRepository.deleteById(salvo.getId());
        }

        if (dto.getContatos() != null) {
            for (ContatoDTO contatoDTO : dto.getContatos()) {
                String telefoneLimpo = contatoDTO.getTelefone() != null ? contatoDTO.getTelefone().replaceAll("\\D", "") : null;
                Contato contato;

                if (contatoDTO.getId() != null) {
                    contato = contatoRepository.findById(contatoDTO.getId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contato vinculado por ID não encontrado."));

                    atualizarDadosContato(contato, contatoDTO, telefoneLimpo);
                    contato = contatoRepository.save(contato);
                } else {
                    contato = contatoRepository.findByTelefone(telefoneLimpo)
                            .map(c -> {
                                atualizarDadosContato(c, contatoDTO, telefoneLimpo);
                                return contatoRepository.save(c);
                            })
                            .orElseGet(() -> {
                                Contato c = new Contato();
                                atualizarDadosContato(c, contatoDTO, telefoneLimpo);
                                return contatoRepository.save(c);
                            });
                }

                ContatoUsuarioId caId = new ContatoUsuarioId(salvo.getId(), contato.getId());
                if (contatoUsuarioRepository.existsById(caId)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Este contato já está vinculado ao usuário.");
                }

                if (contatoDTO.getPrincipal()) {
                    removerPrincipalAtual(salvo);
                }

                ContatoUsuario ca = new ContatoUsuario();
                ca.setId(caId);
                ca.setUsuario(salvo);
                ca.setContato(contato);
                ca.setParentesco(contatoDTO.getParentesco());
                ca.setPrincipal(contatoDTO.getPrincipal());
                contatoUsuarioRepository.save(ca);
            }
        }

        if (dto.getFichaSocioeconomica() != null) {
            FichaSocioeconomica ficha = new FichaSocioeconomica();
            ficha.setIdUsuario(salvo.getId());
            mapearFichaSocioeconomica(ficha, dto.getFichaSocioeconomica());
            fichaSocioeconomicaRepository.save(ficha);
        }

        if (dto.getComposicaoFamiliar() != null) {
            for (ComposicaoFamiliarDTO cfDto : dto.getComposicaoFamiliar()) {
                ComposicaoFamiliar cf = new ComposicaoFamiliar();
                cf.setIdUsuario(salvo.getId());
                cf.setNomeCompleto(cfDto.getNomeCompleto());
                cf.setParentescoVinculo(cfDto.getParentescoVinculo());
                cf.setIdade(cfDto.getIdade());
                cf.setEscolaridade(cfDto.getEscolaridade());
                if (cfDto.getRenda() != null) cf.setRenda(cfDto.getRenda());
                if (cfDto.getBeneficios() != null) cf.setBeneficios(cfDto.getBeneficios());
                composicaoFamiliarRepository.save(cf);
            }
        }

        Turma turma = turmaRepository.findById(dto.getIdTurma())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada."));

        Matricula matricula = new Matricula();
        matricula.setUsuario(salvo);
        matricula.setTurma(turma);
        matricula.setStatus(StatusMatricula.ATIVO);
        matricula.setDataIngresso(LocalDateTime.now());
        matricula = matriculaRepository.save(matricula);

        List<ContatoUsuario> contatosSalvos = contatoUsuarioRepository.findByUsuario(salvo);
        return new UsuarioResponseDTO(salvo, contatosSalvos, matricula);
    }

    @Transactional
    public UsuarioResponseDTO atualizarUsuario(Integer id, AtualizarUsuarioDTO dto, Membro membroLogado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        validarAcessoAoUsuario(usuario, membroLogado);

        if (dto.getCpf() != null && !dto.getCpf().isBlank()) {
            usuarioRepository.findByCpf(dto.getCpf()).ifPresent(u -> {
                if (!u.getId().equals(usuario.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Este CPF já está cadastrado em outro usuário.");
                }
            });
        }

        if (dto.getDocumentoAuxiliar() != null && !dto.getDocumentoAuxiliar().isBlank() && dto.getTipoDocumento() != null) {
            usuarioRepository.findFirstByDocumentoAuxiliarAndTipoDocumento(dto.getDocumentoAuxiliar(), dto.getTipoDocumento()).ifPresent(u -> {
                if (!u.getId().equals(usuario.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Este documento auxiliar já está cadastrado em outro usuário.");
                }
            });
        }

        mapearDadosUsuario(usuario, dto);
        usuarioRepository.save(usuario);

        List<ContatoUsuario> vinculosAtuais = contatoUsuarioRepository.findByUsuario(usuario);
        List<String> telefonesDto = dto.getContatos().stream()
                .map(c -> c.getTelefone() != null ? c.getTelefone().replaceAll("\\D", "") : null)
                .collect(Collectors.toList());

        for (ContatoUsuario ca : vinculosAtuais) {
            if (!telefonesDto.contains(ca.getContato().getTelefone())) {
                contatoUsuarioRepository.delete(ca);
            }
        }

        removerPrincipalAtual(usuario);

        for (ContatoDTO contatoDTO : dto.getContatos()) {
            String telefoneLimpo = contatoDTO.getTelefone() != null ? contatoDTO.getTelefone().replaceAll("\\D", "") : null;
            Contato contato;

            contato = contatoRepository.findByTelefone(telefoneLimpo)
                    .orElseGet(() -> {
                        Contato c = new Contato();
                        c.setTelefone(telefoneLimpo);
                        return c;
                    });

            atualizarDadosContato(contato, contatoDTO, telefoneLimpo);
            contato = contatoRepository.save(contato);

            ContatoUsuarioId caId = new ContatoUsuarioId(usuario.getId(), contato.getId());
            ContatoUsuario ca = contatoUsuarioRepository.findById(caId).orElse(new ContatoUsuario());
            ca.setId(caId);
            ca.setUsuario(usuario);
            ca.setContato(contato);
            ca.setParentesco(contatoDTO.getParentesco());
            ca.setPrincipal(contatoDTO.getPrincipal());
            contatoUsuarioRepository.save(ca);
        }
        
        if (dto.getFichaSocioeconomica() != null) {
            FichaSocioeconomica ficha = fichaSocioeconomicaRepository.findById(usuario.getId()).orElse(new FichaSocioeconomica());
            ficha.setIdUsuario(usuario.getId());
            mapearFichaSocioeconomica(ficha, dto.getFichaSocioeconomica());
            fichaSocioeconomicaRepository.save(ficha);
        }

        if (dto.getComposicaoFamiliar() != null) {
            composicaoFamiliarRepository.deleteByIdUsuario(usuario.getId());
            for (ComposicaoFamiliarDTO cfDto : dto.getComposicaoFamiliar()) {
                ComposicaoFamiliar cf = new ComposicaoFamiliar();
                cf.setIdUsuario(usuario.getId());
                cf.setNomeCompleto(cfDto.getNomeCompleto());
                cf.setParentescoVinculo(cfDto.getParentescoVinculo());
                cf.setIdade(cfDto.getIdade());
                cf.setEscolaridade(cfDto.getEscolaridade());
                if (cfDto.getRenda() != null) cf.setRenda(cfDto.getRenda());
                if (cfDto.getBeneficios() != null) cf.setBeneficios(cfDto.getBeneficios());
                composicaoFamiliarRepository.save(cf);
            }
        }

        Matricula matriculaAtiva = obterMatriculaAtiva(usuario);
        
        if (matriculaAtiva == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O usuário não possui matrícula ativa.");
        }

        if (!matriculaAtiva.getTurma().getId().equals(dto.getIdTurma())) {
            TransferirTurmaDTO transDto = new TransferirTurmaDTO();
            transDto.setIdTurmaNova(dto.getIdTurma());
            transferirTurma(usuario.getId(), transDto, membroLogado);
            
            matriculaAtiva = obterMatriculaAtiva(usuario);
        }

        List<ContatoUsuario> contatosSalvos = contatoUsuarioRepository.findByUsuario(usuario);
        return new UsuarioResponseDTO(usuario, contatosSalvos, matriculaAtiva);
    }

    @Transactional
    public void atualizarFotoPerfil(Integer id, MultipartFile foto, Membro membroLogado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        validarAcessoAoUsuario(usuario, membroLogado);

        arquivoService.validarTipoArquivo(foto, TipoArquivo.FOTO);

        String fotoAntiga = usuario.getImagemPerfil();
        String caminhoFoto = arquivoService.salvarArquivo(foto, "usuarios/", TipoArquivo.FOTO);
        usuario.setImagemPerfil(caminhoFoto);
        
        usuarioRepository.save(usuario);

        if (fotoAntiga != null) {
            arquivoService.deletarArquivo(fotoAntiga);
        }
    }

    @Transactional
    public UsuarioResponseDTO transferirTurma(Integer usuarioId, TransferirTurmaDTO dto, Membro membroLogado) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        validarAcessoAoUsuario(usuario, membroLogado);

        Matricula matriculaAtiva = obterMatriculaAtiva(usuario);
        if (matriculaAtiva == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O usuário não possui matrícula ativa para transferir.");
        }

        if (matriculaAtiva.getTurma().getId().equals(dto.getIdTurmaNova())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O usuário já está matriculado nesta turma.");
        }

        Turma novaTurma = turmaRepository.findById(dto.getIdTurmaNova())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nova turma não encontrada."));

        long horas = ChronoUnit.HOURS.between(matriculaAtiva.getDataIngresso(), LocalDateTime.now());

        if (horas < 24) {
            matriculaAtiva.setTurma(novaTurma);
            matriculaRepository.save(matriculaAtiva);
        } else {
            matriculaAtiva.setStatus(StatusMatricula.INATIVO);
            matriculaAtiva.setDataDesligamento(LocalDate.now());
            matriculaRepository.save(matriculaAtiva);

            Matricula novaMatricula = new Matricula();
            novaMatricula.setUsuario(usuario);
            novaMatricula.setTurma(novaTurma);
            novaMatricula.setStatus(StatusMatricula.ATIVO);
            novaMatricula.setDataIngresso(LocalDateTime.now());
            matriculaAtiva = matriculaRepository.save(novaMatricula);
        }

        List<ContatoUsuario> contatos = contatoUsuarioRepository.findByUsuario(usuario);
        return new UsuarioResponseDTO(usuario, contatos, matriculaAtiva);
    }

    @Transactional
    public void inativarUsuario(Integer usuarioId, InativarUsuarioDTO dto, Membro membroLogado) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        validarAcessoAoUsuario(usuario, membroLogado);

        Matricula matriculaAtiva = obterMatriculaAtiva(usuario);
        if (matriculaAtiva == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O usuário não possui matrícula ativa para inativar.");
        }

        matriculaAtiva.setStatus(StatusMatricula.EGRESSO);
        matriculaAtiva.setDataDesligamento(dto.getDataDesligamento() != null ? dto.getDataDesligamento() : LocalDate.now());
        if (dto.getJustificativa() != null) {
            matriculaAtiva.setJustificativaEgresso(dto.getJustificativa());
        }
        matriculaRepository.save(matriculaAtiva);
    }

    @Transactional
    public UsuarioResponseDTO vincularContatoExistente(Integer usuarioId, Integer contatoId, VincularContatoExistenteDTO dto, Membro membroLogado) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        validarAcessoAoUsuario(usuario, membroLogado);

        Contato contato = contatoRepository.findById(contatoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contato não encontrado."));

        validarLimiteVinculos(usuario);

        ContatoUsuarioId caId = new ContatoUsuarioId(usuario.getId(), contato.getId());
        if (contatoUsuarioRepository.existsById(caId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este contato já está vinculado ao usuário.");
        }

        if (dto.getPrincipal()) {
            removerPrincipalAtual(usuario);
        }

        ContatoUsuario ca = new ContatoUsuario();
        ca.setId(caId);
        ca.setUsuario(usuario);
        ca.setContato(contato);
        ca.setParentesco(dto.getParentesco());
        ca.setPrincipal(dto.getPrincipal());
        contatoUsuarioRepository.save(ca);

        return buscarUsuarioPorId(usuarioId);
    }

    @Transactional
    public UsuarioResponseDTO vincularNovoContato(Integer usuarioId, ContatoDTO dto, Membro membroLogado) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        validarAcessoAoUsuario(usuario, membroLogado);

        validarLimiteVinculos(usuario);

        String telefoneLimpo = dto.getTelefone() != null ? dto.getTelefone().replaceAll("\\D", "") : null;
        Contato contato;

        if (dto.getId() != null) {
            contato = contatoRepository.findById(dto.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contato vinculado por ID não encontrado."));

            atualizarDadosContato(contato, dto, telefoneLimpo);
            contato = contatoRepository.save(contato);
        } else {
            contato = contatoRepository.findByTelefone(telefoneLimpo)
                    .map(c -> {
                        atualizarDadosContato(c, dto, telefoneLimpo);
                        return contatoRepository.save(c);
                    })
                    .orElseGet(() -> {
                        Contato c = new Contato();
                        atualizarDadosContato(c, dto, telefoneLimpo);
                        return contatoRepository.save(c);
                    });
        }

        ContatoUsuarioId caId = new ContatoUsuarioId(usuario.getId(), contato.getId());
        if (contatoUsuarioRepository.existsById(caId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este contato já está vinculado ao usuário.");
        }

        if (dto.getPrincipal()) {
            removerPrincipalAtual(usuario);
        }

        ContatoUsuario ca = new ContatoUsuario();
        ca.setId(caId);
        ca.setUsuario(usuario);
        ca.setContato(contato);
        ca.setParentesco(dto.getParentesco());
        ca.setPrincipal(dto.getPrincipal());
        contatoUsuarioRepository.save(ca);

        return buscarUsuarioPorId(usuarioId);
    }

    @Transactional
    public void atualizarVinculo(Integer usuarioId, Integer contatoId, AtualizarVinculoDTO dto, Membro membroLogado) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
                
        validarAcessoAoUsuario(usuario, membroLogado);
        
        ContatoUsuario vinculo = contatoUsuarioRepository.findByUsuarioIdAndContatoId(usuarioId, contatoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo não encontrado."));

        if (dto.getPrincipal() && !vinculo.getPrincipal()) {
            removerPrincipalAtual(vinculo.getUsuario());
        } else if (!dto.getPrincipal() && vinculo.getPrincipal()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível remover o status de principal diretamente. Marque outro contato como principal para substituí-lo.");
        }

        vinculo.setParentesco(dto.getParentesco());
        vinculo.setPrincipal(dto.getPrincipal());
        contatoUsuarioRepository.save(vinculo);
    }

    @Transactional
    public void desvincularContato(Integer usuarioId, Integer contatoId, Membro membroLogado) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        validarAcessoAoUsuario(usuario, membroLogado);

        ContatoUsuario vinculo = contatoUsuarioRepository.findByUsuarioIdAndContatoId(usuarioId, contatoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo não encontrado."));

        if (vinculo.getPrincipal()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível desvincular o contato principal. Marque outro contato como principal antes.");
        }

        contatoUsuarioRepository.delete(vinculo);
    }

    @Transactional
    public void deletarUsuario(Integer id, Membro membroLogado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        validarAcessoAoUsuario(usuario, membroLogado);

        List<Matricula> matriculas = matriculaRepository.findByUsuario(usuario);
        String fotoAntiga = usuario.getImagemPerfil();
        
        List<ArquivoSaude> arquivosSaude = arquivoSaudeRepository.findByIdUsuario(usuario.getId());
        
        if (matriculas.isEmpty()) {
            limparTabelasFilhasSoftDelete(usuario.getId(), arquivosSaude);
            usuarioRepository.delete(usuario);
            if (fotoAntiga != null) arquivoService.deletarArquivo(fotoAntiga);
            return;
        }

        Matricula matriculaInicial = matriculas.stream()
                .min((m1, m2) -> m1.getDataIngresso().compareTo(m2.getDataIngresso()))
                .orElse(matriculas.get(0));

        long horasDesdeCadastro = ChronoUnit.HOURS.between(matriculaInicial.getDataIngresso(), LocalDateTime.now());

        if (matriculas.size() == 1 && horasDesdeCadastro <= 24) {
            matriculaRepository.deleteAll(matriculas);
            limparTabelasFilhasSoftDelete(usuario.getId(), arquivosSaude);
            usuarioRepository.delete(usuario);
            
            if (fotoAntiga != null) {
                arquivoService.deletarArquivo(fotoAntiga);
            }
        } else {
            limparTabelasFilhasSoftDelete(usuario.getId(), arquivosSaude);

            usuario.setCpf(null);
            usuario.setDocumentoAuxiliar(null);
            usuario.setTipoDocumento(null);
            usuario.setCadUnico(null);
            usuario.setEndereco(null);
            usuario.setBairro(null);
            usuario.setCep(null);
            usuario.setEscola(null);
            usuario.setPeriodoEscolar(null);
            usuario.setSerieEscolar(null);
            usuario.setRaEscolar(null);
            usuario.setImagemPerfil(null);
            usuarioRepository.save(usuario);

            boolean alterouAlgumaAtiva = false;
            for (Matricula m : matriculas) {
                if (m.getStatus() == StatusMatricula.ATIVO) {
                    m.setStatus(StatusMatricula.EXCLUIDO);
                    m.setDataDesligamento(LocalDate.now());
                    matriculaRepository.save(m);
                    alterouAlgumaAtiva = true;
                }
            }

            if (!alterouAlgumaAtiva && !matriculas.isEmpty()) {
                Matricula maisRecente = matriculas.stream()
                        .max((m1, m2) -> m1.getDataIngresso().compareTo(m2.getDataIngresso()))
                        .orElse(matriculas.get(0));

                maisRecente.setStatus(StatusMatricula.EXCLUIDO);
                if (maisRecente.getDataDesligamento() == null) {
                    maisRecente.setDataDesligamento(LocalDate.now());
                }
                matriculaRepository.save(maisRecente);
            }

            if (fotoAntiga != null) {
                arquivoService.deletarArquivo(fotoAntiga);
            }
        }
    }
        
    private void limparTabelasFilhasSoftDelete(Integer usuarioId, List<ArquivoSaude> arquivosSaude) {
        for (ArquivoSaude arq : arquivosSaude) {
            arquivoService.deletarArquivo(arq.getCaminhoArquivo());
        }
        arquivoSaudeRepository.deleteByIdUsuario(usuarioId);
        composicaoFamiliarRepository.deleteByIdUsuario(usuarioId);
        fichaSocioeconomicaRepository.deleteById(usuarioId);
        entrevistaSocialRepository.deleteByIdUsuario(usuarioId);
        contatoUsuarioRepository.deleteByUsuarioId(usuarioId);
    }

    private void validarLimiteVinculos(Usuario usuario) {
        long totalVinculos = contatoUsuarioRepository.countByUsuario(usuario);
        if (totalVinculos >= 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O usuário já atingiu o limite máximo de 4 responsáveis.");
        }
    }

    private void removerPrincipalAtual(Usuario usuario) {
        contatoUsuarioRepository.findByUsuarioAndPrincipalTrue(usuario)
                .ifPresent(antigo -> {
                    antigo.setPrincipal(false);
                    contatoUsuarioRepository.save(antigo);
                });
    }

    private Usuario novoUsuario(CadastroUsuarioCompletoDTO dto) {
        Usuario usuario = new Usuario();
        mapearDadosUsuario(usuario, dto);
        return usuario;
    }
    
    private void mapearDadosUsuario(Usuario usuario, CadastroUsuarioCompletoDTO dto) {
        usuario.setNomeCompleto(dto.getNomeCompleto());
        usuario.setDataNascimento(dto.getDataNascimento());
        if (dto.getCpf() != null && !dto.getCpf().isBlank()) {
            usuario.setCpf(dto.getCpf());
        }
        if (dto.getDocumentoAuxiliar() != null && !dto.getDocumentoAuxiliar().isBlank()) {
            usuario.setDocumentoAuxiliar(dto.getDocumentoAuxiliar());
            usuario.setTipoDocumento(dto.getTipoDocumento());
        }
        usuario.setCadUnico(dto.getCadUnico());
        usuario.setEndereco(dto.getEndereco());
        usuario.setBairro(dto.getBairro());
        usuario.setCep(dto.getCep());
        usuario.setEscola(dto.getEscola());
        usuario.setPeriodoEscolar(dto.getPeriodoEscolar());
        usuario.setSerieEscolar(dto.getSerieEscolar());
        usuario.setRaEscolar(dto.getRaEscolar());
    }

    private void mapearFichaSocioeconomica(FichaSocioeconomica ficha, FichaSocioeconomicaDTO dto) {
        if (dto.getPossuiProblemaSaude() != null) ficha.setPossuiProblemaSaude(dto.getPossuiProblemaSaude());
        ficha.setDescProblemaSaude(dto.getDescProblemaSaude());
        if (dto.getUsaMedicacao() != null) ficha.setUsaMedicacao(dto.getUsaMedicacao());
        ficha.setDescMedicacao(dto.getDescMedicacao());
        if (dto.getTemAlergia() != null) ficha.setTemAlergia(dto.getTemAlergia());
        ficha.setDescAlergia(dto.getDescAlergia());
        ficha.setTipoMoradia(dto.getTipoMoradia());
        if (dto.getValorAluguel() != null) ficha.setValorAluguel(dto.getValorAluguel());
        if (dto.getValorFinanciamento() != null) ficha.setValorFinanciamento(dto.getValorFinanciamento());
        if (dto.getDespesaEnergia() != null) ficha.setDespesaEnergia(dto.getDespesaEnergia());
        if (dto.getDespesaAgua() != null) ficha.setDespesaAgua(dto.getDespesaAgua());
        if (dto.getDespesaInternet() != null) ficha.setDespesaInternet(dto.getDespesaInternet());
        if (dto.getDespesaTelefone() != null) ficha.setDespesaTelefone(dto.getDespesaTelefone());
        if (dto.getDespesaMercado() != null) ficha.setDespesaMercado(dto.getDespesaMercado());
        if (dto.getDespesaFarmacia() != null) ficha.setDespesaFarmacia(dto.getDespesaFarmacia());
        if (dto.getDespesaFinanciamentos() != null) ficha.setDespesaFinanciamentos(dto.getDespesaFinanciamentos());
        if (dto.getDespesaOutras() != null) ficha.setDespesaOutras(dto.getDespesaOutras());
        if (dto.getUtilizaCarro() != null) ficha.setUtilizaCarro(dto.getUtilizaCarro());
        if (dto.getGastoCarro() != null) ficha.setGastoCarro(dto.getGastoCarro());
        if (dto.getUtilizaMoto() != null) ficha.setUtilizaMoto(dto.getUtilizaMoto());
        if (dto.getGastoMoto() != null) ficha.setGastoMoto(dto.getGastoMoto());
        if (dto.getUtilizaTransportePublico() != null) ficha.setUtilizaTransportePublico(dto.getUtilizaTransportePublico());
        if (dto.getGastoTransportePublico() != null) ficha.setGastoTransportePublico(dto.getGastoTransportePublico());
        if (dto.getUtilizaVan() != null) ficha.setUtilizaVan(dto.getUtilizaVan());
        if (dto.getGastoVan() != null) ficha.setGastoVan(dto.getGastoVan());
        if (dto.getAndandoOuBicicleta() != null) ficha.setAndandoOuBicicleta(dto.getAndandoOuBicicleta());
        ficha.setReligiao(dto.getReligiao());
    }

    private void atualizarDadosContato(Contato contato, ContatoDTO dto, String telefoneLimpo) {
        contato.setNomeCompleto(dto.getNomeCompleto());
        contato.setTelefone(telefoneLimpo);
        if (dto.getEmail() != null) contato.setEmail(dto.getEmail());
        if (dto.getEndereco() != null) contato.setEndereco(dto.getEndereco());
        if (dto.getCpf() != null) contato.setCpf(dto.getCpf());
        if (dto.getLocalTrabalho() != null) contato.setLocalTrabalho(dto.getLocalTrabalho());
    }
    
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> buscarUsuariosAutocomplete(String termo, Integer unidadeId, Membro membroLogado) {
        if (!Perfil.ADMINISTRADOR.name().equals(membroLogado.getPapel().getNomePapel())) {
            boolean hasAccess = membroLogado.getUnidades().stream().anyMatch(u -> u.getId().equals(unidadeId));
            if (!hasAccess) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a esta unidade.");
            }
        }
        
        return usuarioRepository.searchAtivosByTermoAndUnidade(termo, unidadeId).stream()
                .map(u -> new UsuarioResponseDTO(u, null, obterMatriculaAtiva(u)))
                .collect(Collectors.toList());
    }

    @Transactional
    public ArquivoSaude uploadArquivoSaude(Integer usuarioId, String titulo, MultipartFile arquivo, Membro membroLogado) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        validarAcessoAoUsuario(usuario, membroLogado);

        List<ArquivoSaude> atuais = arquivoSaudeRepository.findByIdUsuario(usuarioId);
        if (atuais.size() >= 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limite máximo de 4 arquivos de saúde atingido.");
        }

        arquivoService.validarTipoArquivo(arquivo, TipoArquivo.SAUDE);

        String caminho = arquivoService.salvarArquivo(arquivo, "usuarios/saude/", TipoArquivo.SAUDE);

        ArquivoSaude arquivoSaude = new ArquivoSaude();
        arquivoSaude.setIdUsuario(usuario.getId());
        arquivoSaude.setTitulo(titulo);
        arquivoSaude.setCaminhoArquivo(caminho);

        return arquivoSaudeRepository.save(arquivoSaude);
    }
    
    @Transactional(readOnly = true)
    public List<ArquivoSaude> listarArquivosSaude(Integer usuarioId) {
        return arquivoSaudeRepository.findByIdUsuario(usuarioId);
    }

    @Transactional
    public void deletarArquivoSaude(Integer idArquivo, Membro membroLogado) {
        ArquivoSaude arquivo = arquivoSaudeRepository.findById(idArquivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado."));
        
        Usuario usuario = usuarioRepository.findById(arquivo.getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário vinculado a este arquivo não encontrado."));
                
        validarAcessoAoUsuario(usuario, membroLogado);
        
        arquivoService.deletarArquivo(arquivo.getCaminhoArquivo());
        arquivoSaudeRepository.delete(arquivo);
    }
}