package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import br.org.larescolaredencao.dto.EventoResponseDTO;
import br.org.larescolaredencao.model.Evento;
import br.org.larescolaredencao.model.enums.TipoEvento;
import br.org.larescolaredencao.repository.EventoRepository;
import br.org.larescolaredencao.repository.ParceiroRepository;

@ExtendWith(MockitoExtension.class)
class EventoServiceTeste {

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private ArquivoService arquivoService;

    @Mock
    private ParceiroRepository parceiroRepository;

    @InjectMocks
    private EventoService eventoService;

    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void deveListarTodosOsEventosQuandoTipoNaoInformado() {
        Evento evento = new Evento();
        evento.setId(1);
        evento.setTitulo("Bazar Beneficente");
        evento.setTipoEvento(TipoEvento.ARRECADACAO);

        Page<Evento> paginaEsperada = new PageImpl<>(List.of(evento), pageable, 1);
        when(eventoRepository.findAll(pageable)).thenReturn(paginaEsperada);

        Page<EventoResponseDTO> resultado = eventoService.getAllEventos(pageable, null);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getTitulo()).isEqualTo("Bazar Beneficente");
        verify(eventoRepository).findAll(pageable);
        verify(eventoRepository, never()).findByTipoEvento(any(), any());
    }

    @Test
    void deveListarEventosFiltrandoPorTipoQuandoInformado() {
        Evento evento = new Evento();
        evento.setId(2);
        evento.setTitulo("Festa Junina");
        evento.setTipoEvento(TipoEvento.CULTURAL);

        Page<Evento> paginaEsperada = new PageImpl<>(List.of(evento), pageable, 1);
        when(eventoRepository.findByTipoEvento(TipoEvento.CULTURAL, pageable)).thenReturn(paginaEsperada);

        Page<EventoResponseDTO> resultado = eventoService.getAllEventos(pageable, TipoEvento.CULTURAL);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getTipoEvento()).isEqualTo(TipoEvento.CULTURAL);
        verify(eventoRepository).findByTipoEvento(TipoEvento.CULTURAL, pageable);
        verify(eventoRepository, never()).findAll(pageable);
    }

    @Test
    void deveRetornarPaginaVaziaQuandoNaoHaEventos() {
        Page<Evento> paginaVazia = new PageImpl<>(List.of(), pageable, 0);
        when(eventoRepository.findAll(pageable)).thenReturn(paginaVazia);

        Page<EventoResponseDTO> resultado = eventoService.getAllEventos(pageable, null);

        assertThat(resultado.getContent()).isEmpty();
        assertThat(resultado.getTotalElements()).isZero();
    }
}
