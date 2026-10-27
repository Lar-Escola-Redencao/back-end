package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Papel;
import br.org.larescolaredencao.model.Unidade;
import br.org.larescolaredencao.repository.MembroRepository;
import br.org.larescolaredencao.repository.PapelRepository;

@ExtendWith(MockitoExtension.class)
class MembroConsultaPermissoesTest {
    @Mock MembroRepository membros;
    @Mock PapelRepository papeis;
    @Mock UnidadeService unidades;
    @InjectMocks MembroService service;
    Membro coordenador;
    final PageRequest pagina = PageRequest.of(0, 10);

    @BeforeEach
    void preparar() {
        coordenador = membro(10, "COORDENADOR", 1, 2);
    }

    private Membro membro(int id, String papel, Integer... idsUnidades) {
        Membro membro = new Membro();
        membro.setId(id);
        membro.setNomeCompleto("Membro " + id);
        membro.setPapel(new Papel("MONITOR".equals(papel) ? 3 : 2, papel, ""));
        membro.setUnidades(List.of(idsUnidades).stream().map(idUnidade -> {
            Unidade unidade = new Unidade();
            unidade.setId(idUnidade);
            return unidade;
        }).toList());
        return membro;
    }

    private void autenticar() {
        when(membros.findById(10)).thenReturn(Optional.of(coordenador));
    }

    @Test
    void coordenadorUsaConsultaRestritaAsPropriasUnidadesAntesDaPaginacao() {
        autenticar();
        when(membros.listarMonitoresDasUnidades(List.of(1, 2), null, null, null, null, pagina))
                .thenReturn(new PageImpl<>(List.of(membro(20, "MONITOR", 1)), pagina, 15));
        var resposta = service.getAllMembros(pagina, null, null, null, coordenador);
        assertThat(resposta.getContent()).extracting(dto -> dto.getId()).containsExactly(20);
        assertThat(resposta.getTotalElements()).isEqualTo(15);
        verify(membros, never()).findAll(any(PageRequest.class));
        verify(membros, never()).filtrar(any(), any(), any());
    }

    @Test
    void buscaEParametrosDaUrlNaoRetiramRestricaoDasUnidades() {
        autenticar();
        when(membros.listarMonitoresDasUnidades(List.of(1, 2), 1, 2, "%123%", "%123%", pagina))
                .thenReturn(Page.empty(pagina));
        assertThat(service.getAllMembros(pagina, 1, "123", 2, coordenador).getContent()).isEmpty();
        verify(membros).listarMonitoresDasUnidades(List.of(1, 2), 1, 2, "%123%", "%123%", pagina);
    }

    @Test
    void filtroDeUnidadeExternaRetornaVazioSemConsultarListagem() {
        autenticar();
        assertThat(service.getAllMembros(pagina, null, null, 3, coordenador).getTotalElements()).isZero();
        verify(membros).findById(10);
        verifyNoMoreInteractions(membros);
    }

    @Test
    void coordenadorSemUnidadesNaoVeColaboradores() {
        coordenador.setUnidades(List.of());
        autenticar();
        assertThat(service.getAllMembros(pagina, null, null, null, coordenador).getContent()).isEmpty();
        verify(membros).findById(10);
        verifyNoMoreInteractions(membros);
    }

    @Test
    void administradorMantemListagemDeTodosOsColaboradores() {
        coordenador.setPapel(new Papel(1, "ADMINISTRADOR", ""));
        autenticar();
        when(membros.findAll(pagina)).thenReturn(new PageImpl<>(List.of(membro(30, "MONITOR", 3))));
        assertThat(service.getAllMembros(pagina, null, null, null, coordenador).getContent())
                .extracting(dto -> dto.getId()).containsExactly(30);
    }

    @Test
    void administradorSemVinculosPodeFiltrarPorUnidadePreservandoPaginacao() {
        coordenador.setPapel(new Papel(1, "ADMINISTRADOR", ""));
        coordenador.setUnidades(List.of());
        autenticar();
        Membro global = membro(30, "ADMINISTRADOR");
        when(membros.filtrar(null, 3, pagina))
                .thenReturn(new PageImpl<>(List.of(global), pagina, 15));

        var resposta = service.getAllMembros(pagina, null, null, 3, coordenador);

        assertThat(resposta.getContent()).extracting(dto -> dto.getId()).containsExactly(30);
        assertThat(resposta.getContent().get(0).getUnidades()).isEmpty();
        assertThat(resposta.getTotalElements()).isEqualTo(15);
        verify(membros).filtrar(null, 3, pagina);
        verify(membros, never()).listarMonitoresDasUnidades(any(), any(), any(), any(), any(), any());
    }

    @Test
    void administradorSemVinculosCombinaBuscaPapelEUnidade() {
        coordenador.setPapel(new Papel(1, "ADMINISTRADOR", ""));
        coordenador.setUnidades(List.of());
        autenticar();
        when(membros.buscar("%membro%", null, 1, 3, pagina))
                .thenReturn(new PageImpl<>(List.of(membro(30, "ADMINISTRADOR")), pagina, 1));

        var resposta = service.getAllMembros(pagina, 1, "Membro", 3, coordenador);

        assertThat(resposta.getContent()).extracting(dto -> dto.getId()).containsExactly(30);
        verify(membros).buscar("%membro%", null, 1, 3, pagina);
        verify(membros, never()).listarMonitoresDasUnidades(any(), any(), any(), any(), any(), any());
    }

    @Test
    void bloqueiaDetalheDeMonitorDeOutraUnidade() {
        autenticar();
        when(membros.findById(20)).thenReturn(Optional.of(membro(20, "MONITOR", 3)));
        assertThatThrownBy(() -> service.getMembroById(20, coordenador))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void permiteDetalheDeMonitorComUnidadeEmComum() {
        autenticar();
        when(membros.findById(20)).thenReturn(Optional.of(membro(20, "MONITOR", 2, 3)));
        assertThat(service.getMembroById(20, coordenador).getId()).isEqualTo(20);
    }

    @Test
    void bloqueiaDetalheDeOutrosPapeisParaCoordenador() {
        autenticar();
        when(membros.findById(20)).thenReturn(Optional.of(membro(20, "COORDENADOR", 1)));
        assertThatThrownBy(() -> service.getMembroById(20, coordenador))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void naoPermiteListarSemAutenticacao() {
        assertThatThrownBy(() -> service.getAllMembros(pagina, null, null, null, null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verifyNoInteractions(membros);
    }
}
