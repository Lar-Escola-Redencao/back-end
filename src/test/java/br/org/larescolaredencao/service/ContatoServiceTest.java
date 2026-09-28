package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.AtualizarContatoDTO;
import br.org.larescolaredencao.dto.ContatoListagemDTO;
import br.org.larescolaredencao.model.Contato;
import br.org.larescolaredencao.repository.ContatoAssistidoRepository;
import br.org.larescolaredencao.repository.ContatoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ContatoServiceTest {

    @Mock
    private ContatoRepository contatoRepository;

    @Mock
    private ContatoAssistidoRepository contatoAssistidoRepository;

    @InjectMocks
    private ContatoService contatoService;

    private Contato contato;

    @BeforeEach
    void setUp() {
        contato = new Contato();
        contato.setId(1);
        contato.setNomeCompleto("João Silva");
        contato.setTelefone("16999999999");
        contato.setEmail("joao@teste.com");
    }

    @Test
    void criarContatoAvulsoDeveLimparCaracteresDoTelefone() {
        AtualizarContatoDTO dto = new AtualizarContatoDTO();
        dto.setNomeCompleto("João Silva");
        dto.setTelefone("(16) 99999-9999");

        when(contatoRepository.findByTelefone("16999999999")).thenReturn(Optional.empty());
        when(contatoRepository.save(any(Contato.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(contatoAssistidoRepository.countByContatoId(any())).thenReturn(0L);

        ContatoListagemDTO result = contatoService.criarContatoAvulso(dto);

        assertNotNull(result);
        assertEquals("16999999999", result.getTelefone());
        verify(contatoRepository, times(1)).findByTelefone("16999999999");
    }

    @Test
    void atualizarContatoDeveValidarTelefoneUnico() {
        AtualizarContatoDTO dto = new AtualizarContatoDTO();
        dto.setNomeCompleto("João Silva");
        dto.setTelefone("(16) 88888-8888");

        when(contatoRepository.findById(1)).thenReturn(Optional.of(contato));
        when(contatoRepository.findByTelefone("16888888888")).thenReturn(Optional.of(new Contato()));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            contatoService.atualizarContato(1, dto);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void deletarContatoComVinculosDeveLancarBadRequest() {
        when(contatoRepository.findById(1)).thenReturn(Optional.of(contato));
        when(contatoAssistidoRepository.countByContatoId(1)).thenReturn(2L);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            contatoService.deletarContato(1);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(contatoRepository, times(0)).delete(any());
    }

    @Test
    void deletarContatoSemVinculosDeveDeletarComSucesso() {
        when(contatoRepository.findById(1)).thenReturn(Optional.of(contato));
        when(contatoAssistidoRepository.countByContatoId(1)).thenReturn(0L);

        contatoService.deletarContato(1);

        verify(contatoRepository, times(1)).delete(contato);
    }
}