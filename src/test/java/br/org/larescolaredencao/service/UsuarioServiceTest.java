package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.CadastroUsuarioCompletoDTO;
import br.org.larescolaredencao.dto.ComposicaoFamiliarDTO;
import br.org.larescolaredencao.dto.ContatoDTO;
import br.org.larescolaredencao.dto.FichaSocioeconomicaDTO;
import br.org.larescolaredencao.dto.InativarUsuarioDTO;
import br.org.larescolaredencao.dto.TransferirTurmaDTO;
import br.org.larescolaredencao.dto.UsuarioResponseDTO;
import br.org.larescolaredencao.model.ArquivoSaude;
import br.org.larescolaredencao.model.ComposicaoFamiliar;
import br.org.larescolaredencao.model.Contato;
import br.org.larescolaredencao.model.ContatoUsuario;
import br.org.larescolaredencao.model.FichaSocioeconomica;
import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Papel;
import br.org.larescolaredencao.model.Turma;
import br.org.larescolaredencao.model.Unidade;
import br.org.larescolaredencao.model.Usuario;
import br.org.larescolaredencao.model.enums.EscolaridadeFamiliar;
import br.org.larescolaredencao.model.enums.Parentesco;
import br.org.larescolaredencao.model.enums.Periodo;
import br.org.larescolaredencao.model.enums.PeriodoEscolar;
import br.org.larescolaredencao.model.enums.SerieEscolar;
import br.org.larescolaredencao.model.enums.StatusMatricula;
import br.org.larescolaredencao.model.enums.TipoMoradia;
import br.org.larescolaredencao.repository.ArquivoSaudeRepository;
import br.org.larescolaredencao.repository.ComposicaoFamiliarRepository;
import br.org.larescolaredencao.repository.ContatoRepository;
import br.org.larescolaredencao.repository.ContatoUsuarioRepository;
import br.org.larescolaredencao.repository.EntrevistaSocialRepository;
import br.org.larescolaredencao.repository.FichaSocioeconomicaRepository;
import br.org.larescolaredencao.repository.FrequenciaRepository;
import br.org.larescolaredencao.repository.MatriculaRepository;
import br.org.larescolaredencao.repository.MembroRepository;
import br.org.larescolaredencao.repository.OcorrenciaRepository;
import br.org.larescolaredencao.repository.TurmaRepository;
import br.org.larescolaredencao.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private MatriculaRepository matriculaRepository;

    @Mock
    private ContatoRepository contatoRepository;

    @Mock
    private ContatoUsuarioRepository contatoUsuarioRepository;

    @Mock
    private TurmaRepository turmaRepository;

    @Mock
    private ArquivoService arquivoService;

    @Mock
    private FichaSocioeconomicaRepository fichaSocioeconomicaRepository;

    @Mock
    private ComposicaoFamiliarRepository composicaoFamiliarRepository;

    @Mock
    private ArquivoSaudeRepository arquivoSaudeRepository;

    @Mock
    private EntrevistaSocialRepository entrevistaSocialRepository;

    @Mock
    private MembroRepository membroRepository;

    @Mock
    private FrequenciaRepository frequenciaRepository;

    @Mock
    private OcorrenciaRepository ocorrenciaRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;
    private Turma turma;
    private CadastroUsuarioCompletoDTO cadastroDTO;
    private Matricula matriculaAtiva;
    private Membro adminLogado;

    @BeforeEach
    void setUp() {
        Unidade unidade = new Unidade();
        unidade.setId(1);
        unidade.setNome("Sede");

        turma = new Turma();
        turma.setId(1);
        turma.setUnidade(unidade);
        turma.setPeriodo(Periodo.MANHA);
        turma.setHoraInicio(LocalTime.of(8, 0));
        turma.setHoraFim(LocalTime.of(12, 0));

        usuario = new Usuario();
        usuario.setId(1);
        usuario.setNomeCompleto("Enzo Gabriel");
        usuario.setCpf("11122233344");
        usuario.setDataNascimento(LocalDate.of(2015, 5, 10));
        usuario.setImagemPerfil("/uploads/usuarios/foto.jpg");

        matriculaAtiva = new Matricula();
        matriculaAtiva.setId(1);
        matriculaAtiva.setUsuario(usuario);
        matriculaAtiva.setTurma(turma);
        matriculaAtiva.setStatus(StatusMatricula.ATIVO);
        matriculaAtiva.setDataIngresso(LocalDateTime.now());

        ContatoDTO contatoDTO = new ContatoDTO();
        contatoDTO.setNomeCompleto("Maria da Silva");
        contatoDTO.setTelefone("(16) 99999-1111");
        contatoDTO.setParentesco(Parentesco.MAE);
        contatoDTO.setPrincipal(true);

        FichaSocioeconomicaDTO fichaDTO = new FichaSocioeconomicaDTO();
        fichaDTO.setTipoMoradia(TipoMoradia.ALUGADA);
        fichaDTO.setValorAluguel(new BigDecimal("800.00"));

        ComposicaoFamiliarDTO compFamiliarDTO = new ComposicaoFamiliarDTO();
        compFamiliarDTO.setNomeCompleto("José da Silva");
        compFamiliarDTO.setParentescoVinculo(Parentesco.PAI);
        compFamiliarDTO.setEscolaridade(EscolaridadeFamiliar.MEDIO_COMPLETO);

        cadastroDTO = new CadastroUsuarioCompletoDTO();
        cadastroDTO.setNomeCompleto("Enzo Gabriel");
        cadastroDTO.setCpf("11122233344");
        cadastroDTO.setDataNascimento(LocalDate.of(2015, 5, 10));
        cadastroDTO.setEndereco("Rua 1");
        cadastroDTO.setBairro("Centro");
        cadastroDTO.setEscola("Escola Estadual");
        cadastroDTO.setPeriodoEscolar(PeriodoEscolar.MANHA);
        cadastroDTO.setSerieEscolar(SerieEscolar.SERIE_4);
        cadastroDTO.setIdTurma(1);
        cadastroDTO.setContatos(List.of(contatoDTO));
        cadastroDTO.setFichaSocioeconomica(fichaDTO);
        cadastroDTO.setComposicaoFamiliar(List.of(compFamiliarDTO));
        
        Papel adminPapel = new Papel();
        adminPapel.setNomePapel("ADMINISTRADOR");
        
        adminLogado = new Membro();
        adminLogado.setId(999);
        adminLogado.setPapel(adminPapel);
    }

    @Test
    void cadastrarUsuarioDeveCriarNovoUsuarioComMatriculaContatosETabelasAuxiliares() {
        when(usuarioRepository.findByCpf(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(turmaRepository.findById(1)).thenReturn(Optional.of(turma));
        when(contatoRepository.findByTelefone(anyString())).thenReturn(Optional.empty());

        Contato novoContato = new Contato();
        novoContato.setId(1);
        when(contatoRepository.save(any(Contato.class))).thenReturn(novoContato);
        when(matriculaRepository.save(any(Matricula.class))).thenReturn(matriculaAtiva);

        UsuarioResponseDTO response = usuarioService.cadastrarUsuario(cadastroDTO, adminLogado);

        assertNotNull(response);
        assertEquals("Enzo Gabriel", response.getNomeCompleto());
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
        verify(matriculaRepository, times(1)).save(any(Matricula.class));
        verify(contatoUsuarioRepository, times(1)).save(any(ContatoUsuario.class));
        verify(fichaSocioeconomicaRepository, times(1)).save(any(FichaSocioeconomica.class));
        verify(composicaoFamiliarRepository, times(1)).save(any(ComposicaoFamiliar.class));
    }

    @Test
    void cadastrarUsuarioComMatriculaAtivaDeveLancarConflictException() {
        when(usuarioRepository.findByCpf(anyString())).thenReturn(Optional.of(usuario));
        when(matriculaRepository.findByUsuario(usuario)).thenReturn(List.of(matriculaAtiva));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            usuarioService.cadastrarUsuario(cadastroDTO, adminLogado);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void transferirTurmaMenosDe24HorasDeveAtualizarMatriculaAtual() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        
        matriculaAtiva.setDataIngresso(LocalDateTime.now().minusHours(10));
        when(matriculaRepository.findByUsuario(usuario)).thenReturn(List.of(matriculaAtiva));

        Turma novaTurma = new Turma();
        novaTurma.setId(2);
        novaTurma.setPeriodo(Periodo.TARDE);
        novaTurma.setUnidade(turma.getUnidade());
        when(turmaRepository.findById(2)).thenReturn(Optional.of(novaTurma));

        TransferirTurmaDTO dto = new TransferirTurmaDTO();
        dto.setIdTurmaNova(2);
        dto.setDataTransferencia(LocalDate.now());

        usuarioService.transferirTurma(1, dto, adminLogado);

        verify(matriculaRepository, times(1)).save(matriculaAtiva);
        assertEquals(2, matriculaAtiva.getTurma().getId());
        assertEquals(StatusMatricula.ATIVO, matriculaAtiva.getStatus());
    }

    @Test
    void transferirTurmaMaisDe24HorasDeveInativarAtualECriarNova() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        matriculaAtiva.setDataIngresso(LocalDateTime.of(2026, 9, 1, 8, 0));
        when(matriculaRepository.findByUsuario(usuario)).thenReturn(List.of(matriculaAtiva));

        Turma novaTurma = new Turma();
        novaTurma.setId(2);
        novaTurma.setUnidade(turma.getUnidade());
        when(turmaRepository.findById(2)).thenReturn(Optional.of(novaTurma));

        TransferirTurmaDTO dto = new TransferirTurmaDTO();
        dto.setIdTurmaNova(2);
        dto.setDataTransferencia(LocalDate.of(2026, 10, 5));

        usuarioService.transferirTurma(1, dto, adminLogado);

        verify(matriculaRepository, times(2)).save(any(Matricula.class));
        verify(frequenciaRepository, times(1)).deleteByMatriculaIdAndDataRegistroGreaterThanEqual(1, dto.getDataTransferencia());
        verify(ocorrenciaRepository, times(1)).deleteByMatriculaIdAndDataOcorrenciaGreaterThanEqual(1, dto.getDataTransferencia());
        assertEquals(StatusMatricula.INATIVO, matriculaAtiva.getStatus());
        assertEquals(LocalDate.of(2026, 10, 4), matriculaAtiva.getDataDesligamento());
    }

    @Test
    void transferirTurmaComDataAnteriorAoIngressoDeveLancarBadRequest() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        matriculaAtiva.setDataIngresso(LocalDateTime.of(2026, 10, 5, 8, 0));
        when(matriculaRepository.findByUsuario(usuario)).thenReturn(List.of(matriculaAtiva));

        TransferirTurmaDTO dto = new TransferirTurmaDTO();
        dto.setIdTurmaNova(2);
        dto.setDataTransferencia(LocalDate.of(2026, 10, 4));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                usuarioService.transferirTurma(1, dto, adminLogado));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void inativarUsuarioDeveMudarStatusParaEgressoESalvarJustificativa() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(matriculaRepository.findByUsuario(usuario)).thenReturn(List.of(matriculaAtiva));

        InativarUsuarioDTO dto = new InativarUsuarioDTO();
        dto.setDataDesligamento(LocalDate.now());
        dto.setJustificativa("Mudança de cidade");

        usuarioService.inativarUsuario(1, dto, adminLogado);

        verify(matriculaRepository, times(1)).save(matriculaAtiva);
        verify(frequenciaRepository, times(1)).deleteByMatriculaIdAndDataRegistroAfter(1, dto.getDataDesligamento());
        verify(ocorrenciaRepository, times(1)).deleteByMatriculaIdAndDataOcorrenciaAfter(1, dto.getDataDesligamento());
        assertEquals(StatusMatricula.EGRESSO, matriculaAtiva.getStatus());
        assertEquals(dto.getDataDesligamento(), matriculaAtiva.getDataDesligamento());
        assertEquals("Mudança de cidade", matriculaAtiva.getJustificativaEgresso());
    }

    @Test
    void inativarUsuarioComDataAnteriorAoIngressoDeveLancarBadRequest() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        matriculaAtiva.setDataIngresso(LocalDateTime.of(2026, 9, 1, 8, 0));
        when(matriculaRepository.findByUsuario(usuario)).thenReturn(List.of(matriculaAtiva));

        InativarUsuarioDTO dto = new InativarUsuarioDTO();
        dto.setDataDesligamento(LocalDate.of(2020, 1, 1));
        dto.setJustificativa("Mudança de cidade");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                usuarioService.inativarUsuario(1, dto, adminLogado));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void deletarUsuarioMenosDe24HorasHardDeleteETabelasFilhas() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        
        matriculaAtiva.setDataIngresso(LocalDateTime.now().minusHours(5));
        when(matriculaRepository.findByUsuario(usuario)).thenReturn(List.of(matriculaAtiva));
        
        ArquivoSaude arq = new ArquivoSaude();
        arq.setCaminhoArquivo("caminho");
        when(arquivoSaudeRepository.findByIdUsuario(1)).thenReturn(List.of(arq));

        usuarioService.deletarUsuario(1, adminLogado);

        verify(matriculaRepository, times(1)).deleteAll(any());
        verify(arquivoService, times(1)).deletarArquivo("caminho");
        verify(arquivoSaudeRepository, times(1)).deleteByIdUsuario(1);
        verify(composicaoFamiliarRepository, times(1)).deleteByIdUsuario(1);
        verify(fichaSocioeconomicaRepository, times(1)).deleteById(1);
        verify(entrevistaSocialRepository, times(1)).deleteByIdUsuario(1);
        verify(contatoUsuarioRepository, times(1)).deleteByUsuarioId(1);
        verify(usuarioRepository, times(1)).delete(usuario);
    }

    @Test
    void deletarUsuarioMaisDe24HorasSoftDeleteENulaCamposSensiveis() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        
        matriculaAtiva.setDataIngresso(LocalDateTime.now().minusHours(48));
        when(matriculaRepository.findByUsuario(usuario)).thenReturn(List.of(matriculaAtiva));
        when(arquivoSaudeRepository.findByIdUsuario(1)).thenReturn(Collections.emptyList());

        usuarioService.deletarUsuario(1, adminLogado);

        verify(arquivoSaudeRepository, times(1)).deleteByIdUsuario(1);
        verify(composicaoFamiliarRepository, times(1)).deleteByIdUsuario(1);
        verify(fichaSocioeconomicaRepository, times(1)).deleteById(1);
        verify(usuarioRepository, times(1)).save(usuario);
        verify(matriculaRepository, times(1)).save(matriculaAtiva);
        verify(usuarioRepository, times(0)).delete(usuario);
        
        assertEquals(StatusMatricula.EXCLUIDO, matriculaAtiva.getStatus());
        assertNull(usuario.getCpf());
        assertNull(usuario.getEndereco());
        assertNull(usuario.getEscola());
        assertNull(usuario.getImagemPerfil());
    }

    @Test
    void desvincularContatoValidaPermissaoDeCoordenadorDiferenteUnidade() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(matriculaRepository.findByUsuario(usuario)).thenReturn(List.of(matriculaAtiva));
        
        Papel papelCoord = new Papel();
        papelCoord.setNomePapel("COORDENADOR");
        
        Membro coordenador = new Membro();
        coordenador.setPapel(papelCoord);
        Unidade outraUnidade = new Unidade();
        outraUnidade.setId(99); 
        coordenador.setUnidades(List.of(outraUnidade)); 
        
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            usuarioService.desvincularContato(1, 2, coordenador);
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(contatoUsuarioRepository, times(0)).delete(any());
    }

    @Test
    void atualizarFotoDeUsuarioEgressoDeveLancarBadRequest() {
        matriculaAtiva.setStatus(StatusMatricula.EGRESSO);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(matriculaRepository.findByUsuario(usuario)).thenReturn(List.of(matriculaAtiva));

        MockMultipartFile foto = new MockMultipartFile("foto", "foto.png", "image/png", "conteudo".getBytes());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                usuarioService.atualizarFotoPerfil(1, foto, adminLogado));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(arquivoService, never()).salvarArquivo(any(), anyString(), any());
        verify(usuarioRepository, never()).save(usuario);
    }

    @Test
    void uploadArquivoSaudeDeUsuarioEgressoDeveLancarBadRequest() {
        matriculaAtiva.setStatus(StatusMatricula.EGRESSO);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(matriculaRepository.findByUsuario(usuario)).thenReturn(List.of(matriculaAtiva));

        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "laudo.pdf", "application/pdf", "conteudo".getBytes());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                usuarioService.uploadArquivoSaude(1, "Laudo", arquivo, adminLogado));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(arquivoService, never()).salvarArquivo(any(), anyString(), any());
        verify(arquivoSaudeRepository, never()).save(any());
    }

    @Test
    void vincularContatoEmUsuarioEgressoDeveLancarBadRequest() {
        matriculaAtiva.setStatus(StatusMatricula.EGRESSO);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(matriculaRepository.findByUsuario(usuario)).thenReturn(List.of(matriculaAtiva));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                usuarioService.vincularContatoExistente(1, 2, new br.org.larescolaredencao.dto.VincularContatoExistenteDTO(), adminLogado));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(contatoUsuarioRepository, never()).save(any());
    }

}
