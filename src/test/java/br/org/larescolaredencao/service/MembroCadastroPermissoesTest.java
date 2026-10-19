package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import br.org.larescolaredencao.dto.CriarMembroDTO;
import br.org.larescolaredencao.model.*;
import br.org.larescolaredencao.repository.*;

@ExtendWith(MockitoExtension.class)
class MembroCadastroPermissoesTest {
    @Mock MembroRepository membros;
    @Mock PapelRepository papeis;
    @Mock UnidadeService unidades;
    @InjectMocks MembroService service;
    Membro coordenador;
    CriarMembroDTO dto;

    @BeforeEach
    void preparar() {
        coordenador = new Membro();
        coordenador.setId(10);
        coordenador.setPapel(new Papel(2, "COORDENADOR", ""));
        coordenador.setUnidades(List.of(unidade(1), unidade(2)));
        dto = new CriarMembroDTO();
        dto.setIdPapel(3);
        dto.setEmail("monitor@teste.com");
        dto.setCpf("12345678900");
        dto.setSenha("Senha123!");
    }

    private Unidade unidade(int id) {
        Unidade unidade = new Unidade();
        unidade.setId(id);
        return unidade;
    }

    private void autenticar(String papelDestino) {
        when(membros.findById(10)).thenReturn(Optional.of(coordenador));
        when(papeis.findById(3)).thenReturn(Optional.of(new Papel(3, papelDestino, "")));
    }

    @Test
    void permiteTodasAsUnidadesVinculadasAoCoordenador() {
        autenticar("MONITOR");
        dto.setIdsUnidades(List.of(1, 2));
        when(unidades.buscarUnidadesPorIds(List.of(1, 2))).thenReturn(coordenador.getUnidades());
        when(membros.save(any(Membro.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.criarMembro(dto, coordenador).getUnidades())
                .extracting(Unidade::getId).containsExactly(1, 2);
    }

    @Test
    void bloqueiaRequisicaoComUnidadeNaoPermitidaMesmoMisturadaComPermitida() {
        autenticar("MONITOR");
        dto.setIdsUnidades(List.of(1, 3));
        assertThatThrownBy(() -> service.criarMembro(dto, coordenador))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(membros, never()).save(any());
        verifyNoInteractions(unidades);
    }

    @Test
    void bloqueiaCriacaoDeOutroCoordenador() {
        autenticar("COORDENADOR");
        dto.setIdsUnidades(List.of(1));
        assertThatThrownBy(() -> service.criarMembro(dto, coordenador))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(membros, never()).save(any());
    }

    @Test
    void administradorPodeCadastrarEmUnidadeForaDosPropriosVinculos() {
        coordenador.setPapel(new Papel(1, "ADMINISTRADOR", ""));
        autenticar("MONITOR");
        dto.setIdsUnidades(List.of(3));
        when(unidades.buscarUnidadesPorIds(List.of(3))).thenReturn(List.of(unidade(3)));
        when(membros.save(any(Membro.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.criarMembro(dto, coordenador).getUnidades())
                .extracting(Unidade::getId).containsExactly(3);
    }

    @Test
    void rejeitaCadastroSemAutenticacao() {
        assertThatThrownBy(() -> service.criarMembro(dto, null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(membros, never()).save(any());
    }

    @Test
    void rejeitaSolicitanteQueNaoExisteMaisNoBanco() {
        when(membros.findById(10)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.criarMembro(dto, coordenador))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(membros, never()).save(any());
        verifyNoInteractions(papeis, unidades);
    }

    @Test
    void consultaPermissaoAtualNoBancoEmVezDeConfiarNoPerfilDoToken() {
        Membro solicitanteDoToken = new Membro();
        solicitanteDoToken.setId(10);
        solicitanteDoToken.setPapel(new Papel(1, "ADMINISTRADOR", ""));
        coordenador.setPapel(new Papel(3, "MONITOR", ""));
        when(membros.findById(10)).thenReturn(Optional.of(coordenador));

        assertThatThrownBy(() -> service.criarMembro(dto, solicitanteDoToken))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(membros, never()).save(any());
        verifyNoInteractions(papeis, unidades);
    }
}
