package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import br.org.larescolaredencao.model.MidiaEvento;
import br.org.larescolaredencao.model.Parceiro;
import br.org.larescolaredencao.model.enums.TipoEvento;
import br.org.larescolaredencao.model.enums.TipoMidia;
import br.org.larescolaredencao.repository.EventoRepository;
import br.org.larescolaredencao.repository.MidiaEventoRepository;
import br.org.larescolaredencao.repository.ParceiroRepository;

@ExtendWith(MockitoExtension.class)
class EventoServiceTeste {

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private ArquivoService arquivoService;

    @Mock
    private ParceiroRepository parceiroRepository;

    @Mock
    private MidiaEventoRepository midiaEventoRepository;

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
    void deveDetalharEventoSemCoberturaComComentarioNuloEListasVazias() {
        Evento evento = new Evento();
        evento.setId(3);
        evento.setTitulo("Bazar de Primavera");
        when(eventoRepository.findByIdComParceiros(3)).thenReturn(Optional.of(evento));
        when(midiaEventoRepository.findByEventoIdOrderByIdAsc(3)).thenReturn(List.of());

        EventoDetalhadoResponseDTO resultado = eventoService.getEventoById(3);

        assertThat(resultado.getTitulo()).isEqualTo("Bazar de Primavera");
        assertThat(resultado.getComentarioPosEvento()).isNull();
        assertThat(resultado.getMidiaEvento()).isEmpty();
        assertThat(resultado.getParceiros()).isEmpty();
    }

    @Test
    void deveDetalharEventoComCoberturaEIncluirParceiroInativo() {
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
        evento.setComentarioPosEvento("Arrecadamos 200 kg de alimentos.");
        evento.setParceiros(List.of(ativo, inativo));

        MidiaEvento foto = new MidiaEvento();
        foto.setId(10);
        foto.setTipoMidia(TipoMidia.IMAGEM);
        foto.setUrlMidia("/uploads/eventos/midias/foto.jpg");

        when(eventoRepository.findByIdComParceiros(4)).thenReturn(Optional.of(evento));
        when(midiaEventoRepository.findByEventoIdOrderByIdAsc(4)).thenReturn(List.of(foto));

        EventoDetalhadoResponseDTO resultado = eventoService.getEventoById(4);

        assertThat(resultado.getComentarioPosEvento()).isEqualTo("Arrecadamos 200 kg de alimentos.");
        assertThat(resultado.getMidiaEvento()).hasSize(1);
        assertThat(resultado.getMidiaEvento().get(0).getId()).isEqualTo(10);
        assertThat(resultado.getMidiaEvento().get(0).getTipoMidia()).isEqualTo(TipoMidia.IMAGEM);
        assertThat(resultado.getMidiaEvento().get(0).getUrlMidia()).isEqualTo("/uploads/eventos/midias/foto.jpg");
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
        verify(midiaEventoRepository, never()).findByEventoIdOrderByIdAsc(any());
    }
}
