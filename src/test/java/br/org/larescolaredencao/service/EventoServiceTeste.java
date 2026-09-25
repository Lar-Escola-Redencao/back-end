package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.EventoDetalhadoResponseDTO;
import br.org.larescolaredencao.dto.EventoResponseDTO;
import br.org.larescolaredencao.model.Evento;
import br.org.larescolaredencao.model.Parceiro;
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

    @Test
    void deveDetalharEventoSemParceiros() {
        Evento evento = new Evento();
        evento.setId(3);
        evento.setTitulo("Bazar de Primavera");
        when(eventoRepository.findByIdComParceiros(3)).thenReturn(Optional.of(evento));

        EventoDetalhadoResponseDTO resultado = eventoService.getEventoById(3);

        assertThat(resultado.getTitulo()).isEqualTo("Bazar de Primavera");
        assertThat(resultado.getParceiros()).isEmpty();
    }

    @Test
    void deveDetalharEventoIncluindoParceiroInativo() {
        Parceiro ativo = new Parceiro();
        ativo.setId(1L);
        ativo.setNome("Padaria Central");
        ativo.setAtivo(true);

        // parceria encerrada hoje, mas apoiou esta edição: tem que aparecer no histórico.
        Parceiro inativo = new Parceiro();
        inativo.setId(2L);
        inativo.setNome("Mercado Antigo");
        inativo.setAtivo(false);

        Evento evento = new Evento();
        evento.setId(4);
        evento.setParceiros(List.of(ativo, inativo));

        when(eventoRepository.findByIdComParceiros(4)).thenReturn(Optional.of(evento));

        EventoDetalhadoResponseDTO resultado = eventoService.getEventoById(4);

        assertThat(resultado.getParceiros()).extracting("nome").containsExactly("Padaria Central", "Mercado Antigo");
    }

    @Test
    void deveRetornar404QuandoEventoNaoExiste() {
        when(eventoRepository.findByIdComParceiros(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventoService.getEventoById(999))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Evento não encontrado")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deveDetalharEventoComoEncerradoQuandoDataJaPassou() {
        Evento evento = new Evento();
        evento.setId(5);
        evento.setDataEvento(LocalDateTime.now().minusDays(1));
        when(eventoRepository.findByIdComParceiros(5)).thenReturn(Optional.of(evento));

        EventoDetalhadoResponseDTO resultado = eventoService.getEventoById(5);

        assertThat(resultado.isEncerrado()).isTrue();
    }

    @Test
    void deveDetalharEventoComoNaoEncerradoQuandoDataAindaNaoChegou() {
        Evento evento = new Evento();
        evento.setId(6);
        evento.setDataEvento(LocalDateTime.now().plusDays(1));
        when(eventoRepository.findByIdComParceiros(6)).thenReturn(Optional.of(evento));

        EventoDetalhadoResponseDTO resultado = eventoService.getEventoById(6);

        assertThat(resultado.isEncerrado()).isFalse();
    }

    @Test
    void ehEventoEncerradoDeveCompararComADataDeReferencia() {
        LocalDateTime dataEvento = LocalDateTime.of(2026, 9, 24, 19, 0);
        Evento evento = new Evento();
        evento.setDataEvento(dataEvento);

        assertThat(evento.ehEventoEncerrado(dataEvento.minusMinutes(1))).isFalse();
        assertThat(evento.ehEventoEncerrado(dataEvento)).isFalse();
        assertThat(evento.ehEventoEncerrado(dataEvento.plusMinutes(1))).isTrue();
    }
}
