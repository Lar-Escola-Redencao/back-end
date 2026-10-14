package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.AlterarSenhaDTO;
import br.org.larescolaredencao.dto.CriarMembroDTO;
import br.org.larescolaredencao.dto.AtualizarMembroDTO;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Papel;
import br.org.larescolaredencao.model.Unidade;
import br.org.larescolaredencao.repository.MembroRepository;
import br.org.larescolaredencao.repository.PapelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class MembroCorrecoesTest {
    @Mock
    private MembroRepository membros;
    @Mock
    private PapelRepository papeis;
    @Mock
    private UnidadeService unidades;
    MembroService service;
    Membro coordenador;

    @BeforeEach
    void preparar() {
        service = new MembroService(membros, papeis, unidades);
        coordenador = new Membro();
        coordenador.setPapel(new Papel(2, "COORDENADOR", "Coordenador"));
        Unidade unidade = new Unidade();
        unidade.setId(1);
        coordenador.setUnidades(List.of(unidade));
    }

    @Test
    void coordenadorNaoPodeCriarAdministrador() {
        CriarMembroDTO dto = new CriarMembroDTO();
        dto.setIdPapel(1);
        when(papeis.findById(1)).thenReturn(Optional.of(new Papel(1, "ADMINISTRADOR", "Admin")));
        assertThatThrownBy(() -> service.criarMembro(dto, coordenador))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verifyNoInteractions(membros);
    }

    @Test
    void coordenadorNaoPodePromoverMonitorNemEditarAdministrador() {
        Membro alvo = new Membro();
        alvo.setPapel(new Papel(3, "MONITOR", "Monitor"));
        alvo.setUnidades(coordenador.getUnidades());
        when(membros.findById(10)).thenReturn(Optional.of(alvo));
        when(papeis.findById(1)).thenReturn(Optional.of(new Papel(1, "ADMINISTRADOR", "Admin")));
        AtualizarMembroDTO dto = new AtualizarMembroDTO();
        dto.setIdPapel(1);
        assertThatThrownBy(() -> service.atualizarMembro(10, dto, coordenador))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        alvo.setPapel(new Papel(1, "ADMINISTRADOR", "Admin"));
        assertThatThrownBy(() -> service.deletarMembro(10, coordenador)).isInstanceOf(ResponseStatusException.class);
        verify(membros, never()).save(any());
        verify(membros, never()).delete(any());
    }

    @Test
    void coordenadorNaoPodeVincularMonitorAOutraUnidade() {
        when(papeis.findById(3)).thenReturn(Optional.of(new Papel(3, "MONITOR", "Monitor")));
        CriarMembroDTO dto = new CriarMembroDTO();
        dto.setIdPapel(3);
        dto.setIdsUnidades(List.of(2));
        assertThatThrownBy(() -> service.criarMembro(dto, coordenador)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(membros);
    }

    @Test
    void listaDeUnidadesPermaneceMutavelAoEditarAdministrador() {
        Membro membro = new Membro();
        membro.setUnidades(List.of());
        assertThatCode(() -> membro.getUnidades().clear()).doesNotThrowAnyException();
        assertThatCode(() -> membro.getUnidades().add(new Unidade())).doesNotThrowAnyException();
    }

    @Test
    void alterarSenhaValidaAtualEConfirmacaoEPersisteHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Membro membro = new Membro();
        membro.setSenha(encoder.encode("admin123"));
        when(membros.findById(10)).thenReturn(Optional.of(membro));
        AlterarSenhaDTO dto = new AlterarSenhaDTO();
        dto.setSenhaAtual("errada");
        dto.setNovaSenha("Nova123");
        dto.setConfirmacaoSenha("Nova123");
        assertThatThrownBy(() -> service.alterarSenha(10, dto)).isInstanceOf(ResponseStatusException.class);
        dto.setSenhaAtual("admin123");
        dto.setConfirmacaoSenha("Diferente123");
        assertThatThrownBy(() -> service.alterarSenha(10, dto)).isInstanceOf(ResponseStatusException.class);
        verify(membros, never()).save(any());
        dto.setConfirmacaoSenha("Nova123");
        service.alterarSenha(10, dto);
        assertThat(encoder.matches("Nova123", membro.getSenha())).isTrue();
        assertThat(encoder.matches("admin123", membro.getSenha())).isFalse();
        verify(membros).save(membro);
    }
}
