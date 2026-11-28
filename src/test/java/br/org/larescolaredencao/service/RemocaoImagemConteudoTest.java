package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.*;
import br.org.larescolaredencao.model.*;
import br.org.larescolaredencao.model.enums.TipoEvento;
import br.org.larescolaredencao.repository.*;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RemocaoImagemConteudoTest {
    @Test void removeImagemDoEventoSomenteAoAtualizarComIndicador() {
        var eventos = mock(EventoRepository.class);
        var arquivos = mock(ArquivoService.class);
        var evento = new Evento(); evento.setId(1); evento.setImagem("/uploads/eventos/foto.jpg");
        when(eventos.findById(1)).thenReturn(Optional.of(evento));
        when(eventos.save(evento)).thenReturn(evento);
        var service = new EventoService(eventos, arquivos, mock(ParceiroRepository.class),
                mock(RedeSocialRepository.class), mock(EventoRedeSocialRepository.class));
        var dto = new AtualizarEventoDTO(); dto.setCategorias(Set.of(TipoEvento.CULTURAL));
        service.atualizarEvento(1, dto);
        assertThat(evento.getImagem()).isEqualTo("/uploads/eventos/foto.jpg");
        verifyNoInteractions(arquivos);
        dto.setRemoverImagem(true);
        service.atualizarEvento(1, dto);
        assertThat(evento.getImagem()).isNull();
        verify(arquivos).deletarArquivoAposCommit("/uploads/eventos/foto.jpg");
    }

    @Test void removeLogoDoParceiroSomenteAoAtualizarComIndicador() {
        var parceiros = mock(ParceiroRepository.class);
        var arquivos = mock(ArquivoService.class);
        var parceiro = new Parceiro(); parceiro.setLogo("/uploads/parceiros/logo.jpg");
        when(parceiros.findById(1L)).thenReturn(Optional.of(parceiro));
        when(parceiros.save(parceiro)).thenReturn(parceiro);
        var service = new ParceiroService(parceiros, arquivos, mock(EventoRepository.class));
        var dto = new AtualizarParceiroDTO();
        service.atualizarParceiro(1L, dto);
        assertThat(parceiro.getLogo()).isEqualTo("/uploads/parceiros/logo.jpg");
        verifyNoInteractions(arquivos);
        dto.setRemoverLogo(true);
        service.atualizarParceiro(1L, dto);
        assertThat(parceiro.getLogo()).isNull();
        verify(arquivos).deletarArquivoAposCommit("/uploads/parceiros/logo.jpg");
    }

    @Test void falhaAoSalvarDiretoriaNaoAgendaExclusaoDoArquivo() {
        var diretorias = mock(DiretoriaRepository.class);
        var arquivos = mock(ArquivoService.class);
        var diretoria = new Diretoria(); diretoria.setFoto("/uploads/diretoria/foto.jpg");
        when(diretorias.findById(1L)).thenReturn(Optional.of(diretoria));
        when(diretorias.save(diretoria)).thenThrow(new IllegalStateException("Falha"));
        var service = new DiretoriaService(diretorias, arquivos);
        var dto = new AtualizarDiretoriaDTO(); dto.setRemoverFoto(true);
        assertThatThrownBy(() -> service.atualizar(1L, dto)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(arquivos);
    }

    @Test void removeFotoDaDiretoriaAoSalvar() {
        var diretorias = mock(DiretoriaRepository.class);
        var arquivos = mock(ArquivoService.class);
        var diretoria = new Diretoria(); diretoria.setFoto("/uploads/diretoria/foto.jpg");
        when(diretorias.findById(1L)).thenReturn(Optional.of(diretoria));
        when(diretorias.save(diretoria)).thenReturn(diretoria);
        var dto = new AtualizarDiretoriaDTO(); dto.setRemoverFoto(true);
        new DiretoriaService(diretorias, arquivos).atualizar(1L, dto);
        assertThat(diretoria.getFoto()).isNull();
        verify(arquivos).deletarArquivoAposCommit("/uploads/diretoria/foto.jpg");
    }
}
