package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.AlterarSenhaDTO;
import br.org.larescolaredencao.dto.CriarMembroDTO;
import br.org.larescolaredencao.dto.AtualizarMembroDTO;
import br.org.larescolaredencao.dto.AtualizarPerfilDTO;
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

    @Test
    void emailEquivalenteECpfComMascaraNaoConsultamDuplicidadeNemAlteramIdentificadores() {
        var admin = new Membro(1, "Administrador", "admin@exemplo.com", "hash", "52998224725", "Rua A", "11999999999",
                new Papel(1, "ADMINISTRADOR", "Administrador"));
        when(membros.findById(1)).thenReturn(Optional.of(admin));
        when(membros.save(admin)).thenReturn(admin);
        var dto = new AtualizarPerfilDTO();
        dto.setNomeCompleto("Administrador"); dto.setEndereco("Rua A"); dto.setTelefone("11999999999");
        dto.setEmail(" ADMIN@EXEMPLO.COM "); dto.setCpf("529.982.247-25");
        service.atualizarPerfil(1, dto);
        assertThat(admin.getEmail()).isEqualTo("admin@exemplo.com");
        assertThat(admin.getCpf()).isEqualTo("52998224725");
        verify(membros, never()).findByEmail(any());
        verify(membros, never()).buscarPorCpf(any());
    }

    @Test
    void administradorEditaIdentificadoresSemAlterarPapelOuUnidades() {
        var admin = new Membro(1, "Administrador", "admin@exemplo.com", "hash", "52998224725", "Rua A", "11999999999",
                new Papel(1, "ADMINISTRADOR", "Administrador"));
        admin.setUnidades(List.of(new Unidade()));
        var vinculos = admin.getUnidades();
        when(membros.findById(1)).thenReturn(Optional.of(admin));
        when(membros.save(admin)).thenReturn(admin);
        var dto = new AtualizarPerfilDTO();
        dto.setNomeCompleto("Administrador");
        dto.setEndereco("Rua A");
        dto.setTelefone("11999999999");
        dto.setEmail("novo@exemplo.com");
        dto.setCpf("111.444.777-35");
        service.atualizarPerfil(1, dto);
        assertThat(admin.getEmail()).isEqualTo("novo@exemplo.com");
        assertThat(admin.getCpf()).isEqualTo("11144477735");
        assertThat(admin.getPapel().getNomePapel()).isEqualTo("ADMINISTRADOR");
        assertThat(admin.getUnidades()).isSameAs(vinculos);
        verifyNoInteractions(papeis, unidades);
    }

    @Test
    void coordenadorNaoAlteraCpfOuEmailPeloProprioPerfil() {
        coordenador.setEmail("coord@exemplo.com");
        coordenador.setCpf("52998224725");
        when(membros.findById(20)).thenReturn(Optional.of(coordenador));
        for (boolean alterarEmail : new boolean[] { true, false }) {
            var dto = new AtualizarPerfilDTO();
            dto.setNomeCompleto("Coordenador");
            if (alterarEmail) dto.setEmail("novo@exemplo.com");
            else dto.setCpf("11144477735");
            assertThatThrownBy(() -> service.atualizarPerfil(20, dto))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        }
        verify(membros, never()).save(any());
        assertThat(coordenador.getEmail()).isEqualTo("coord@exemplo.com");
    }

    @Test
    void rejeitaEmailOuCpfDuplicadoAntesDeAlterarOPerfil() {
        var admin = new Membro(1, "Administrador", "admin@exemplo.com", "hash", "52998224725", "Rua A", "11999999999",
                new Papel(1, "ADMINISTRADOR", "Administrador"));
        when(membros.findById(1)).thenReturn(Optional.of(admin));
        var outro = new Membro(); outro.setId(2);
        var dto = new AtualizarPerfilDTO(); dto.setNomeCompleto("Novo nome");
        dto.setEmail("ocupado@exemplo.com");
        when(membros.findByEmail(dto.getEmail())).thenReturn(Optional.of(outro));
        assertThatThrownBy(() -> service.atualizarPerfil(1, dto))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        dto.setEmail(null); dto.setCpf("11144477735");
        when(membros.buscarPorCpf(dto.getCpf())).thenReturn(Optional.of(outro));
        assertThatThrownBy(() -> service.atualizarPerfil(1, dto))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(admin.getNomeCompleto()).isEqualTo("Administrador");
        verify(membros, never()).save(any());
    }
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
        coordenador.setId(20);
        coordenador.setPapel(new Papel(2, "COORDENADOR", "Coordenador"));
        Unidade unidade = new Unidade();
        unidade.setId(1);
        coordenador.setUnidades(List.of(unidade));
    }

    @Test
    void coordenadorNaoPodeCriarAdministrador() {
        CriarMembroDTO dto = new CriarMembroDTO();
        dto.setIdPapel(1);
        when(membros.findById(20)).thenReturn(Optional.of(coordenador));
        when(papeis.findById(1)).thenReturn(Optional.of(new Papel(1, "ADMINISTRADOR", "Admin")));
        assertThatThrownBy(() -> service.criarMembro(dto, coordenador))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(membros, never()).save(any());
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
        when(membros.findById(20)).thenReturn(Optional.of(coordenador));
        when(papeis.findById(3)).thenReturn(Optional.of(new Papel(3, "MONITOR", "Monitor")));
        CriarMembroDTO dto = new CriarMembroDTO();
        dto.setIdPapel(3);
        dto.setIdsUnidades(List.of(2));
        assertThatThrownBy(() -> service.criarMembro(dto, coordenador)).isInstanceOf(ResponseStatusException.class);
        verify(membros, never()).save(any());
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

    @Test
    void administradorNaoPodeEditarOuExcluirAPropriaContaPelaGestao() {
        Membro administrador = new Membro();
        administrador.setId(1);
        administrador.setPapel(new Papel(1, "ADMINISTRADOR", "Administrador"));

        assertThatThrownBy(() -> service.atualizarMembro(1, new AtualizarMembroDTO(), administrador))
                .isInstanceOfSatisfying(ResponseStatusException.class, erro -> {
                    assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(erro.getReason()).contains("Meu perfil");
                });
        assertThatThrownBy(() -> service.deletarMembro(1, administrador))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verifyNoInteractions(membros, papeis, unidades);
    }

    @Test
    void editarSomenteNomePreservaColecaoEVinculosDeUnidades() {
        Membro monitor = new Membro(10, "Monitor", "monitor@exemplo.com", "hash",
                "12345678900", "Rua A", "11999999999", new Papel(3, "MONITOR", "Monitor"));
        monitor.setUnidades(coordenador.getUnidades());
        List<Unidade> vinculosOriginais = monitor.getUnidades();
        AtualizarMembroDTO dto = new AtualizarMembroDTO();
        dto.setNomeCompleto("Monitor atualizado");
        dto.setEmail(monitor.getEmail());
        dto.setCpf(monitor.getCpf());
        dto.setEndereco(monitor.getEndereco());
        dto.setTelefone(monitor.getTelefone());
        dto.setIdPapel(3);
        dto.setIdsUnidades(List.of(1));
        when(membros.findById(10)).thenReturn(Optional.of(monitor));
        when(papeis.findById(3)).thenReturn(Optional.of(monitor.getPapel()));
        when(unidades.buscarUnidadesPorIds(List.of(1))).thenReturn(coordenador.getUnidades());
        when(membros.save(monitor)).thenReturn(monitor);

        service.atualizarMembro(10, dto, coordenador);

        assertThat(monitor.getNomeCompleto()).isEqualTo("Monitor atualizado");
        assertThat(monitor.getUnidades()).isSameAs(vinculosOriginais).containsExactlyElementsOf(coordenador.getUnidades());
    }
    @Test
    void omitirUnidadesPreservaVinculosDoAdministrador() {
        Membro administrador = prepararEdicaoComUnidade("ADMINISTRADOR", 1);
        var vinculos = administrador.getUnidades();

        service.atualizarMembro(10, edicao(administrador, null));

        assertThat(administrador.getUnidades()).isSameAs(vinculos).hasSize(1);
        assertThat(administrador.getUnidades().getFirst().getId()).isEqualTo(1);
        verifyNoInteractions(unidades);
    }

    @Test
    void coordenadorPodeEditarNomeDoMonitorSemReenviarUnidades() {
        Membro monitor = prepararEdicaoComUnidade("MONITOR", 3);

        service.atualizarMembro(10, edicao(monitor, null), coordenador);

        assertThat(monitor.getNomeCompleto()).isEqualTo("Nome atualizado");
        assertThat(monitor.getUnidades()).hasSize(1);
        verifyNoInteractions(unidades);
    }

    @Test
    void listaVaziaExplicitaRemoveUnidadesDoAdministrador() {
        Membro administrador = prepararEdicaoComUnidade("ADMINISTRADOR", 1);

        service.atualizarMembro(10, edicao(administrador, List.of()));

        assertThat(administrador.getUnidades()).isEmpty();
    }

    @Test
    void listaVaziaExplicitaNaoPodeRemoverTodasUnidadesDoMonitor() {
        Membro monitor = prepararEdicaoComUnidade("MONITOR", 3);

        assertThatThrownBy(() -> service.atualizarMembro(10, edicao(monitor, List.of())))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        assertThat(monitor.getUnidades()).hasSize(1);
        verify(membros, never()).save(any());
    }

    private Membro prepararEdicaoComUnidade(String nomePapel, int idPapel) {
        Membro membro = new Membro(10, "Nome original", "pessoa@exemplo.com", "hash",
                "12345678900", "Rua A", "11999999999", new Papel(idPapel, nomePapel, nomePapel));
        membro.setUnidades(coordenador.getUnidades());
        when(membros.findById(10)).thenReturn(Optional.of(membro));
        when(papeis.findById(idPapel)).thenReturn(Optional.of(membro.getPapel()));
        lenient().when(membros.save(membro)).thenReturn(membro);
        return membro;
    }

    private AtualizarMembroDTO edicao(Membro membro, List<Integer> idsUnidades) {
        AtualizarMembroDTO dto = new AtualizarMembroDTO();
        dto.setNomeCompleto("Nome atualizado");
        dto.setEmail(membro.getEmail());
        dto.setCpf(membro.getCpf());
        dto.setEndereco(membro.getEndereco());
        dto.setTelefone(membro.getTelefone());
        dto.setIdPapel(membro.getPapel().getId());
        dto.setIdsUnidades(idsUnidades);
        return dto;
    }

}
