package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.AssistidoResponseDTO;
import br.org.larescolaredencao.dto.ContatoDTO;
import br.org.larescolaredencao.dto.CriarAssistidoDTO;
import br.org.larescolaredencao.dto.TransferirTurmaDTO;
import br.org.larescolaredencao.model.Assistido;
import br.org.larescolaredencao.model.Contato;
import br.org.larescolaredencao.model.ContatoAssistido;
import br.org.larescolaredencao.model.ContatoAssistidoId;
import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.Turma;
import br.org.larescolaredencao.model.enums.Parentesco;
import br.org.larescolaredencao.model.enums.StatusMatricula;
import br.org.larescolaredencao.repository.AssistidoRepository;
import br.org.larescolaredencao.repository.ContatoAssistidoRepository;
import br.org.larescolaredencao.repository.ContatoRepository;
import br.org.larescolaredencao.repository.MatriculaRepository;
import br.org.larescolaredencao.repository.TurmaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AssistidoServiceTest {

    @Mock
    private AssistidoRepository assistidoRepository;

    @Mock
    private MatriculaRepository matriculaRepository;

    @Mock
    private ContatoRepository contatoRepository;

    @Mock
    private ContatoAssistidoRepository contatoAssistidoRepository;

    @Mock
    private TurmaRepository turmaRepository;

    @Mock
    private ArquivoService arquivoService;

    @InjectMocks
    private AssistidoService assistidoService;

    private Assistido assistido;
    private Turma turma;
    private CriarAssistidoDTO criarAssistidoDTO;

    @BeforeEach
    void setUp() {
        assistido = new Assistido();
        assistido.setId(1);
        assistido.setNomeCompleto("Enzo Gabriel");
        assistido.setCpf("11122233344");
        assistido.setDataNascimento(LocalDate.of(2015, 5, 10));

        turma = new Turma();
        turma.setId(1);

        ContatoDTO contatoDTO = new ContatoDTO();
        contatoDTO.setNomeCompleto("Maria da Silva");
        contatoDTO.setTelefone("(16) 99999-1111");
        contatoDTO.setParentesco(Parentesco.MAE);
        contatoDTO.setPrincipal(true);

        criarAssistidoDTO = new CriarAssistidoDTO();
        criarAssistidoDTO.setNomeCompleto("Enzo Gabriel");
        criarAssistidoDTO.setCpf("11122233344");
        criarAssistidoDTO.setDataNascimento(LocalDate.of(2015, 5, 10));
        criarAssistidoDTO.setIdTurma(1);
        criarAssistidoDTO.setContatos(List.of(contatoDTO));
    }

    @Test
    void cadastrarAssistido_DeveCriarNovoAssistidoComMatriculaEContato() {
        when(assistidoRepository.findByCpf(anyString())).thenReturn(Optional.empty());
        when(assistidoRepository.save(any(Assistido.class))).thenReturn(assistido);
        when(turmaRepository.findById(1)).thenReturn(Optional.of(turma));
        when(contatoRepository.findByTelefone(anyString())).thenReturn(Optional.empty());

        Contato novoContato = new Contato();
        novoContato.setId(1);
        novoContato.setTelefone("(16) 99999-1111");
        when(contatoRepository.save(any(Contato.class))).thenReturn(novoContato);

        when(contatoAssistidoRepository.existsById(any())).thenReturn(false);
        when(contatoAssistidoRepository.findByAssistido(any(Assistido.class))).thenReturn(Collections.emptyList());

        AssistidoResponseDTO response = assistidoService.cadastrarAssistido(criarAssistidoDTO);

        assertNotNull(response);
        assertEquals("Enzo Gabriel", response.getNomeCompleto());
        verify(assistidoRepository, times(1)).save(any(Assistido.class));
        verify(matriculaRepository, times(1)).save(any(Matricula.class));
        verify(contatoAssistidoRepository, times(1)).save(any(ContatoAssistido.class));
    }

    @Test
    void cadastrarAssistido_ComMatriculaAtiva_DeveLancarConflictException() {
        when(assistidoRepository.findByCpf(anyString())).thenReturn(Optional.of(assistido));

        Matricula matriculaAtiva = new Matricula();
        matriculaAtiva.setStatus(StatusMatricula.ATIVO);
        when(matriculaRepository.findByAssistido(assistido)).thenReturn(List.of(matriculaAtiva));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            assistidoService.cadastrarAssistido(criarAssistidoDTO);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Assistido já possui matrícula ATIVA.", exception.getReason());
    }

    @Test
    void cadastrarAssistido_RetornoDeEgresso_DeveReativarCriandoNovaMatricula() {
        when(assistidoRepository.findByCpf(anyString())).thenReturn(Optional.of(assistido));

        Matricula matriculaInativa = new Matricula();
        matriculaInativa.setStatus(StatusMatricula.EGRESSO);
        when(matriculaRepository.findByAssistido(assistido)).thenReturn(List.of(matriculaInativa));

        when(assistidoRepository.save(any(Assistido.class))).thenReturn(assistido);
        when(turmaRepository.findById(1)).thenReturn(Optional.of(turma));
        when(contatoRepository.findByTelefone(anyString())).thenReturn(Optional.empty());

        Contato novoContato = new Contato();
        novoContato.setId(1);
        when(contatoRepository.save(any(Contato.class))).thenReturn(novoContato);

        when(contatoAssistidoRepository.existsById(any())).thenReturn(false);
        when(contatoAssistidoRepository.findByAssistido(any(Assistido.class))).thenReturn(Collections.emptyList());

        AssistidoResponseDTO response = assistidoService.cadastrarAssistido(criarAssistidoDTO);

        assertNotNull(response);
        verify(assistidoRepository, times(1)).save(assistido);
        verify(matriculaRepository, times(1)).save(any(Matricula.class));
    }

    @Test
    void cadastrarAssistido_RetornoDeEgresso_ComMesmoTelefoneNaoDeveLancarConflict() {
        when(assistidoRepository.findByCpf(anyString())).thenReturn(Optional.of(assistido));

        Matricula matriculaInativa = new Matricula();
        matriculaInativa.setStatus(StatusMatricula.EGRESSO);
        when(matriculaRepository.findByAssistido(assistido)).thenReturn(List.of(matriculaInativa));

        when(assistidoRepository.save(any(Assistido.class))).thenReturn(assistido);
        when(turmaRepository.findById(1)).thenReturn(Optional.of(turma));

        Contato contatoExistente = new Contato();
        contatoExistente.setId(9);
        contatoExistente.setTelefone("(16) 99999-1111");
        when(contatoRepository.findByTelefone(anyString())).thenReturn(Optional.of(contatoExistente));
        when(contatoRepository.save(any(Contato.class))).thenReturn(contatoExistente);

        ContatoAssistido vinculoAntigo = new ContatoAssistido();
        vinculoAntigo.setId(new ContatoAssistidoId(assistido.getId(), contatoExistente.getId()));
        vinculoAntigo.setAssistido(assistido);
        vinculoAntigo.setContato(contatoExistente);
        vinculoAntigo.setPrincipal(true);

        when(contatoAssistidoRepository.findByAssistido(assistido))
                .thenReturn(List.of(vinculoAntigo))
                .thenReturn(Collections.emptyList());
        when(contatoAssistidoRepository.existsById(any())).thenReturn(false);

        AssistidoResponseDTO response = assistidoService.cadastrarAssistido(criarAssistidoDTO);

        assertNotNull(response);
        verify(contatoAssistidoRepository, times(1)).deleteAll(List.of(vinculoAntigo));
        verify(contatoAssistidoRepository, times(1)).save(any(ContatoAssistido.class));
    }

    @Test
    void transferirTurma_MenosDe24Horas_DeveAtualizarMatriculaAtual() {
        when(assistidoRepository.findById(1)).thenReturn(Optional.of(assistido));

        Turma turmaAtual = new Turma();
        turmaAtual.setId(1);

        Matricula matriculaAtiva = new Matricula();
        matriculaAtiva.setTurma(turmaAtual);
        matriculaAtiva.setStatus(StatusMatricula.ATIVO);
        matriculaAtiva.setDataIngresso(LocalDateTime.now().minusHours(23).minusMinutes(59));

        when(matriculaRepository.findByAssistido(assistido)).thenReturn(List.of(matriculaAtiva));

        Turma novaTurma = new Turma();
        novaTurma.setId(2);
        when(turmaRepository.findById(2)).thenReturn(Optional.of(novaTurma));

        TransferirTurmaDTO dto = new TransferirTurmaDTO();
        dto.setIdTurmaNova(2);

        assistidoService.transferirTurma(1, dto);

        verify(matriculaRepository, times(1)).save(matriculaAtiva);
        assertEquals(2, matriculaAtiva.getTurma().getId());
        assertEquals(StatusMatricula.ATIVO, matriculaAtiva.getStatus());
    }

    @Test
    void transferirTurma_MaisDe24Horas_DeveInativarAtualECriarNova() {
        when(assistidoRepository.findById(1)).thenReturn(Optional.of(assistido));

        Turma turmaAtual = new Turma();
        turmaAtual.setId(1);

        Matricula matriculaAtiva = new Matricula();
        matriculaAtiva.setTurma(turmaAtual);
        matriculaAtiva.setStatus(StatusMatricula.ATIVO);
        matriculaAtiva.setDataIngresso(LocalDateTime.now().minusHours(30));

        when(matriculaRepository.findByAssistido(assistido)).thenReturn(List.of(matriculaAtiva));

        Turma novaTurma = new Turma();
        novaTurma.setId(2);
        when(turmaRepository.findById(2)).thenReturn(Optional.of(novaTurma));

        TransferirTurmaDTO dto = new TransferirTurmaDTO();
        dto.setIdTurmaNova(2);

        assistidoService.transferirTurma(1, dto);

        verify(matriculaRepository, times(2)).save(any(Matricula.class));
        assertEquals(StatusMatricula.INATIVO, matriculaAtiva.getStatus());
        assertNotNull(matriculaAtiva.getDataDesligamento());
    }

    @Test
    void vincularNovoContato_LimiteAtingido_DeveLancarBadRequest() {
        when(assistidoRepository.findById(1)).thenReturn(Optional.of(assistido));
        when(contatoAssistidoRepository.countByAssistido(assistido)).thenReturn(4L);

        ContatoDTO dto = new ContatoDTO();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            assistidoService.vincularNovoContato(1, dto);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("O assistido já atingiu o limite máximo de 4 responsáveis.", exception.getReason());
    }
}