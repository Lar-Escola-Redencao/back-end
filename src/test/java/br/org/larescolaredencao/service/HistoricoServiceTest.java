package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;
import br.org.larescolaredencao.dto.FiltroHistoricoDTO;
import br.org.larescolaredencao.model.enums.TipoRegistroHistorico;
import br.org.larescolaredencao.repository.HistoricoRepository;
import br.org.larescolaredencao.repository.HistoricoRepository.Linha;

class HistoricoServiceTest {

    private final HistoricoRepository historicoRepository = mock(HistoricoRepository.class);
    private final HistoricoService historicoService = new HistoricoService(historicoRepository);

    @Test
    void rejeitaIdDaFrequenciaAntesDeConsultarBanco() {
        var filtro = filtro(TipoRegistroHistorico.PRESENCAS, 12, null, null, null);
        assertThatThrownBy(() -> historicoService.listar(filtro, PageRequest.of(0, 20)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("ID da frequência");
        verifyNoInteractions(historicoRepository);
    }

    @Test
    void rejeitaPeriodoInvertidoEDataDeAulaEmOutraCategoria() {
        assertThatThrownBy(() -> HistoricoService.validar(filtro(TipoRegistroHistorico.USUARIOS, null,
                LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1), null)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("data final");
        assertThatThrownBy(() -> HistoricoService.validar(filtro(TipoRegistroHistorico.CONTATOS, null,
                null, null, LocalDate.of(2026, 9, 1))))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("somente para Presenças");
    }

    @Test
    void detalhesRetemNomeHistoricoEExibemApenasCamposAlterados() {
        when(historicoRepository.buscar(31)).thenReturn(Optional.of(linha("EDICAO",
                "{\"id\":7,\"nomeCompleto\":\"Ana Antiga\",\"email\":null}",
                "{\"id\":7,\"nomeCompleto\":\"Ana Nova\",\"email\":null}")));
        var detalhe = historicoService.detalhar(31);
        assertThat(detalhe.referenciaAnteriorDisponivel()).isTrue();
        assertThat(detalhe.evento().nomeResponsavel()).isEqualTo("Responsável histórico");
        assertThat(detalhe.campos()).singleElement().satisfies(campo -> {
            assertThat(campo.campo()).isEqualTo("nomeCompleto");
            assertThat(campo.valorAnterior()).isEqualTo("Ana Antiga");
            assertThat(campo.valorNovo()).isEqualTo("Ana Nova");
            assertThat(campo.anteriorConhecido()).isTrue();
        });
    }

    @Test
    void diferenciaReferenciaDesconhecidaDeCampoVazio() {
        when(historicoRepository.buscar(31)).thenReturn(Optional.of(linha("EDICAO", null,
                "{\"id\":7,\"email\":null}")));
        var detalhe = historicoService.detalhar(31);
        assertThat(detalhe.referenciaAnteriorDisponivel()).isFalse();
        assertThat(detalhe.observacao()).contains("indisponível");
        assertThat(detalhe.campos()).extracting(campo -> campo.campo()).contains("email");
        assertThat(detalhe.campos()).allSatisfy(campo -> assertThat(campo.anteriorConhecido()).isFalse());
    }

    @Test
    void exclusaoMantemValoresAnterioresSemDependerDoCadastroAtual() {
        when(historicoRepository.buscar(31)).thenReturn(Optional.of(linha("EXCLUSAO",
                "{\"id\":7,\"nomeCompleto\":\"Ana Excluída\"}", null)));
        var detalhe = historicoService.detalhar(31);
        assertThat(detalhe.evento().registroExcluido()).isTrue();
        assertThat(detalhe.evento().identificacaoRegistro()).contains("Ana Excluída");
        assertThat(detalhe.campos()).filteredOn(campo -> campo.campo().equals("nomeCompleto")).singleElement()
                .satisfies(campo -> assertThat(campo.valorAnterior()).isEqualTo("Ana Excluída"));
        verify(historicoRepository).buscar(31);
        verifyNoMoreInteractions(historicoRepository);
    }

    private static Linha linha(String tipo, String anterior, String novo) {
        return new Linha(31, 2, 0L, 9, "Responsável histórico", "CONTATOS", "contato", "7", 7,
                tipo, anterior, novo, "{}", "{}");
    }

    private static FiltroHistoricoDTO filtro(TipoRegistroHistorico tipo, Integer id, LocalDate inicio,
                                            LocalDate fim, LocalDate aula) {
        return new FiltroHistoricoDTO(tipo, null, id, null, inicio, fim, null, aula);
    }
}
