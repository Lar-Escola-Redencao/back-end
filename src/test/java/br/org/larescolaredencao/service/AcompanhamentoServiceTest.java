package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.EntrevistaDTO;
import br.org.larescolaredencao.model.*;
import br.org.larescolaredencao.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AcompanhamentoServiceTest {
    final UsuarioService usuarios = mock(UsuarioService.class);
    final OcorrenciaRepository ocorrencias = mock(OcorrenciaRepository.class);
    final EntrevistaSocialRepository entrevistas = mock(EntrevistaSocialRepository.class);
    final MembroRepository membros = mock(MembroRepository.class);
    final AcompanhamentoService service = new AcompanhamentoService(usuarios, ocorrencias, entrevistas, membros,
            Clock.fixed(Instant.parse("2026-10-29T15:00:00Z"), ZoneId.of("America/Sao_Paulo")));

    Membro membro(String perfil) {
        var membro = new Membro();
        membro.setId(7);
        membro.setNomeCompleto("Ana");
        var papel = new Papel(); papel.setNomePapel(perfil); membro.setPapel(papel);
        return membro;
    }

    @Test void monitorNaoPodeConsultarOuGravarEntrevistas() {
        var monitor = membro("MONITOR");
        assertThatThrownBy(() -> service.listarEntrevistas(10, null, PageRequest.of(0, 10), monitor))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        assertThatThrownBy(() -> service.salvarEntrevista(10, null, dados(), monitor))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        assertThatThrownBy(() -> service.excluirEntrevista(10, 3, monitor))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        verifyNoInteractions(entrevistas);
    }

    @Test void cadastroDefineAutorPelaSessaoEDataPeloServidor() {
        var admin = membro("ADMINISTRADOR");
        when(entrevistas.save(any())).thenAnswer(i -> i.getArgument(0));
        when(membros.findById(7)).thenReturn(Optional.of(admin));
        var resultado = service.salvarEntrevista(10, null, dados(), admin);
        assertThat(resultado.idMembro()).isEqualTo(7);
        assertThat(resultado.dataRegistro()).isNotNull();
        assertThat(resultado.dataEntrevista()).isEqualTo(dados().dataEntrevista());
        verify(usuarios).buscarUsuarioPorId(10, admin);
    }

    @Test void coordenadorEditaEntrevistaDeOutroAutorSemSubstituirAutoria() {
        var entrevista = new EntrevistaSocial();
        entrevista.setId(3); entrevista.setIdUsuario(10); entrevista.setIdMembro(4);
        var cadastro = LocalDateTime.of(2025, 1, 1, 9, 0);
        entrevista.setDataRegistro(cadastro);
        when(entrevistas.findById(3)).thenReturn(Optional.of(entrevista));
        when(entrevistas.save(any())).thenAnswer(i -> i.getArgument(0));
        var resultado = service.salvarEntrevista(10, 3, dados(), membro("COORDENADOR"));
        assertThat(resultado.idMembro()).isEqualTo(4);
        assertThat(resultado.dataRegistro()).isEqualTo(cadastro);
    }

    @Test void rejeitaEntrevistaDeOutroUsuario() {
        var entrevista = new EntrevistaSocial(); entrevista.setIdUsuario(11);
        when(entrevistas.findById(3)).thenReturn(Optional.of(entrevista));
        assertThatThrownBy(() -> service.excluirEntrevista(10, 3, membro("ADMINISTRADOR")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        verify(entrevistas, never()).delete(any());
    }

    @Test void acessoNegadoAoUsuarioImpedeLeituraDasEntrevistas() {
        var coord = membro("COORDENADOR");
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN))
                .when(usuarios).buscarUsuarioPorId(10, coord);
        assertThatThrownBy(() -> service.listarEntrevistas(10, "Ana", PageRequest.of(0, 10), coord))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(entrevistas);
    }

    EntrevistaDTO dados() { return new EntrevistaDTO(LocalDateTime.of(2026, 10, 29, 9, 30), " Relato "); }

    @Test void rejeitaDiaOuHorarioFuturoNoCadastroENaEdicaoParaAmbosOsPerfis() {
        for (String perfil : new String[] { "ADMINISTRADOR", "COORDENADOR" }) {
            for (Integer id : new Integer[] { null, 3 }) {
                for (LocalDateTime data : new LocalDateTime[] {
                        LocalDateTime.of(2026, 10, 29, 12, 1), LocalDateTime.of(2026, 10, 30, 9, 0) }) {
                    assertThatThrownBy(() -> service.salvarEntrevista(10, id,
                            new EntrevistaDTO(data, "Relato"), membro(perfil)))
                            .isInstanceOf(ResponseStatusException.class)
                            .hasMessageContaining("400").hasMessageContaining("futuro");
                }
            }
        }
        verifyNoInteractions(entrevistas);
    }

    @Test void permiteDataEHoraAtualEmBrasilia() {
        when(entrevistas.save(any())).thenAnswer(i -> i.getArgument(0));
        var agora = LocalDateTime.of(2026, 10, 29, 12, 0);
        var resultado = service.salvarEntrevista(10, null, new EntrevistaDTO(agora, "Relato"), membro("ADMINISTRADOR"));
        assertThat(resultado.dataEntrevista()).isEqualTo(agora);
    }
}
