package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.AssistidoResponseDTO;
import br.org.larescolaredencao.dto.ContatoDTO;
import br.org.larescolaredencao.dto.CriarAssistidoDTO;
import br.org.larescolaredencao.dto.InativarAssistidoDTO;
import br.org.larescolaredencao.dto.TransferirTurmaDTO;
import br.org.larescolaredencao.model.Assistido;
import br.org.larescolaredencao.model.Contato;
import br.org.larescolaredencao.model.ContatoAssistido;
import br.org.larescolaredencao.model.ContatoAssistidoId;
import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.Turma;
import br.org.larescolaredencao.model.Unidade;
import br.org.larescolaredencao.model.enums.Parentesco;
import br.org.larescolaredencao.model.enums.Periodo;
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
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private Matricula matriculaAtiva;

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

        assistido = new Assistido();
        assistido.setId(1);
        assistido.setNomeCompleto("Enzo Gabriel");
        assistido.setCpf("11122233344");
        assistido.setDataNascimento(LocalDate.of(2015, 5, 10));

        matriculaAtiva = new Matricula();
        matriculaAtiva.setId(1);
        matriculaAtiva.setAssistido(assistido);
        matriculaAtiva.setTurma(turma);
        matriculaAtiva.setStatus(StatusMatricula.ATIVO);
        matriculaAtiva.setDataIngresso(LocalDateTime.now());

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
    void cadastrarAssistidoDeveCriarNovoAssistidoComMatriculaEContato() {
        when(assistidoRepository.findByCpf(anyString())).thenReturn(Optional.empty());
        when(assistidoRepository.save(any(Assistido.class))).thenReturn(assistido);
        when(turmaRepository.findById(1)).thenReturn(Optional.of(turma));
        when(contatoRepository.findByTelefone(anyString())).thenReturn(Optional.empty());

        Contato novoContato = new Contato();
        novoContato.setId(1);
        novoContato.setTelefone("16999991111");
        when(contatoRepository.save(any(Contato.class))).thenReturn(novoContato);

        when(contatoAssistidoRepository.existsById(any())).thenReturn(false);
        when(contatoAssistidoRepository.findByAssistido(any(Assistido.class))).thenReturn(Collections.emptyList());
        when(matriculaRepository.save(any(Matricula.class))).thenReturn(matriculaAtiva);

        AssistidoResponseDTO response = assistidoService.cadastrarAssistido(criarAssistidoDTO);

        assertNotNull(response);
        assertEquals("Enzo Gabriel", response.getNomeCompleto());
        verify(assistidoRepository, times(1)).save(any(Assistido.class));
        verify(matriculaRepository, times(1)).save(any(Matricula.class));
        verify(contatoAssistidoRepository, times(1)).save(any(ContatoAssistido.class));
    }

    @Test
    void cadastrarAssistidoComMatriculaAtivaDeveLancarConflictException() {
        when(assistidoRepository.findByCpf(anyString())).thenReturn(Optional.of(assistido));
        when(matriculaRepository.findByAssistido(assistido)).thenReturn(List.of(matriculaAtiva));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            assistidoService.cadastrarAssistido(criarAssistidoDTO);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void transferirTurmaMenosDe24HorasDeveAtualizarMatriculaAtual() {
        when(assistidoRepository.findById(1)).thenReturn(Optional.of(assistido));
        
        matriculaAtiva.setDataIngresso(LocalDateTime.now().minusHours(10));
        when(matriculaRepository.findByAssistido(assistido)).thenReturn(List.of(matriculaAtiva));

        Turma novaTurma = new Turma();
        novaTurma.setId(2);
        novaTurma.setPeriodo(Periodo.TARDE);
        novaTurma.setUnidade(turma.getUnidade());
        when(turmaRepository.findById(2)).thenReturn(Optional.of(novaTurma));

        TransferirTurmaDTO dto = new TransferirTurmaDTO();
        dto.setIdTurmaNova(2);

        assistidoService.transferirTurma(1, dto);

        verify(matriculaRepository, times(1)).save(matriculaAtiva);
        assertEquals(2, matriculaAtiva.getTurma().getId());
        assertEquals(StatusMatricula.ATIVO, matriculaAtiva.getStatus());
    }

    @Test
    void transferirTurmaMaisDe24HorasDeveInativarAtualECriarNova() {
        when(assistidoRepository.findById(1)).thenReturn(Optional.of(assistido));

        matriculaAtiva.setDataIngresso(LocalDateTime.now().minusHours(30));
        when(matriculaRepository.findByAssistido(assistido)).thenReturn(List.of(matriculaAtiva));

        Turma novaTurma = new Turma();
        novaTurma.setId(2);
        novaTurma.setPeriodo(Periodo.TARDE);
        novaTurma.setUnidade(turma.getUnidade());
        when(turmaRepository.findById(2)).thenReturn(Optional.of(novaTurma));

        TransferirTurmaDTO dto = new TransferirTurmaDTO();
        dto.setIdTurmaNova(2);

        assistidoService.transferirTurma(1, dto);

        verify(matriculaRepository, times(2)).save(any(Matricula.class));
        assertEquals(StatusMatricula.INATIVO, matriculaAtiva.getStatus());
        assertNotNull(matriculaAtiva.getDataDesligamento());
    }

    @Test
    void inativarAssistidoDeveMudarStatusParaEgresso() {
        when(assistidoRepository.findById(1)).thenReturn(Optional.of(assistido));
        when(matriculaRepository.findByAssistido(assistido)).thenReturn(List.of(matriculaAtiva));

        InativarAssistidoDTO dto = new InativarAssistidoDTO();
        dto.setDataDesligamento(LocalDate.now());

        assistidoService.inativarAssistido(1, dto);

        verify(matriculaRepository, times(1)).save(matriculaAtiva);
        assertEquals(StatusMatricula.EGRESSO, matriculaAtiva.getStatus());
        assertEquals(dto.getDataDesligamento(), matriculaAtiva.getDataDesligamento());
    }

    @Test
    void deletarAssistidoMenosDe24HorasHardDelete() {
        when(assistidoRepository.findById(1)).thenReturn(Optional.of(assistido));
        
        matriculaAtiva.setDataIngresso(LocalDateTime.now().minusHours(5));
        when(matriculaRepository.findByAssistido(assistido)).thenReturn(List.of(matriculaAtiva));
        
        ContatoAssistido ca = new ContatoAssistido();
        when(contatoAssistidoRepository.findByAssistido(assistido)).thenReturn(List.of(ca));

        assistidoService.deletarAssistido(1);

        verify(matriculaRepository, times(1)).deleteAll(any());
        verify(contatoAssistidoRepository, times(1)).deleteAll(any());
        verify(assistidoRepository, times(1)).delete(assistido);
    }

    @Test
    void deletarAssistidoMaisDe24HorasSoftDelete() {
        when(assistidoRepository.findById(1)).thenReturn(Optional.of(assistido));
        
        matriculaAtiva.setDataIngresso(LocalDateTime.now().minusHours(48));
        when(matriculaRepository.findByAssistido(assistido)).thenReturn(List.of(matriculaAtiva));

        assistidoService.deletarAssistido(1);

        verify(assistidoRepository, times(1)).save(assistido);
        verify(matriculaRepository, times(1)).save(matriculaAtiva);
        verify(assistidoRepository, times(0)).delete(assistido);
        assertEquals(StatusMatricula.EXCLUIDO, matriculaAtiva.getStatus());
    }
}