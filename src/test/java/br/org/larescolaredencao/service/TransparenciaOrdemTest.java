package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.CriarSecaoDTO;
import br.org.larescolaredencao.dto.ReordenarSecaoDTO;
import br.org.larescolaredencao.model.Pagina;
import br.org.larescolaredencao.model.Secao;
import br.org.larescolaredencao.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransparenciaOrdemTest {
    @Mock PaginaRepository paginas;
    @Mock SecaoRepository secoes;
    @Mock DocumentoRepository documentos;
    @Mock ArquivoService arquivos;
    @InjectMocks PaginaService service;

    private Secao secao(long id, Integer ordem) {
        var s = new Secao(); s.setId(id); s.setOrdem(ordem);
        var p = new Pagina(); p.setId(1L); s.setPagina(p);
        return s;
    }
    private ReordenarSecaoDTO posicao(long id, int ordem) {
        var dto = new ReordenarSecaoDTO(); dto.setId(id); dto.setOrdem(ordem); return dto;
    }
    private void lista(Secao... lista) {
        when(secoes.findByPaginaIdComBloqueio(1L)).thenReturn(List.of(lista));
    }

    @Test void rejeitaIdDeOutraPaginaSemAlterarALista() {
        var a = secao(10, 1); var b = secao(20, 2); lista(a, b);
        assertThatThrownBy(() -> service.reordenarSecoes(List.of(posicao(20, 1), posicao(99, 2))))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(a.getOrdem()).isEqualTo(1); assertThat(b.getOrdem()).isEqualTo(2);
    }
    @Test void rejeitaListaIncompletaOuPosicoesRepetidas() {
        lista(secao(10, 1), secao(20, 2));
        for (var pedido : List.of(List.of(posicao(10, 1)),
                List.of(posicao(10, 1), posicao(20, 1)),
                List.of(posicao(10, 1), posicao(10, 2)),
                List.of(posicao(10, 0), posicao(20, 2)))) {
            assertThatThrownBy(() -> service.reordenarSecoes(pedido)).isInstanceOf(ResponseStatusException.class);
        }
    }
    @Test void aplicaPermutacaoCompletaAposValidar() {
        var a = secao(10, 1); var b = secao(20, 2); lista(a, b);
        service.reordenarSecoes(List.of(posicao(20, 1), posicao(10, 2)));
        assertThat(a.getOrdem()).isEqualTo(2); assertThat(b.getOrdem()).isEqualTo(1);
        var sequencia = inOrder(paginas, secoes);
        sequencia.verify(paginas).findByIdComBloqueio(1L);
        sequencia.verify(secoes).findByPaginaIdComBloqueio(1L);
    }
    @Test void inicializaPosicoesAusentesPreservandoASequenciaAtual() {
        var a = secao(10, null); var b = secao(20, 1); var c = secao(30, 1);
        lista(a, b, c);
        service.listarSecoesParaOrdenacao();
        assertThat(List.of(a, b, c)).extracting(Secao::getOrdem).containsExactly(1, 2, 3);
    }
    @Test void novaSecaoVaiAoFinalMesmoSeClienteEnviaOrdem() {
        var p = new Pagina(); p.setId(1L);
        when(paginas.findById(1L)).thenReturn(Optional.of(p));
        lista(secao(10, 1), secao(20, 2));
        when(arquivos.sanitizarTexto("Nova seção")).thenReturn("Nova seção");
        when(secoes.save(any())).thenAnswer(i -> i.getArgument(0));
        var dto = new CriarSecaoDTO(); dto.setTitulo("Nova seção"); dto.setOrdem(1);
        assertThat(service.criarSecao(1L, dto).getOrdem()).isEqualTo(3);
    }
}
