package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.*;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import br.org.larescolaredencao.dto.CriarMembroDTO;
import br.org.larescolaredencao.dto.ReativarMembroDTO;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Papel;
import br.org.larescolaredencao.model.Unidade;
import br.org.larescolaredencao.repository.MembroRepository;
import br.org.larescolaredencao.repository.PapelRepository;

@ExtendWith(MockitoExtension.class)
class MembroDesativacaoTest {
    @Mock MembroRepository membros;
    @Mock PapelRepository papeis;
    @Mock UnidadeService unidades;
    @InjectMocks MembroService service;
    Membro admin;
    Membro coordenador;
    final PageRequest pagina = PageRequest.of(0, 10);

    @BeforeEach
    void preparar() {
        admin = membro(1, "ADMINISTRADOR");
        coordenador = membro(10, "COORDENADOR", 1, 2);
    }

    private Membro membro(int id, String papel, Integer... idsUnidades) {
        Membro membro = new Membro();
        membro.setId(id);
        membro.setNomeCompleto("Membro " + id);
        membro.setEmail("m" + id + "@teste.com");
        membro.setCpf("1234567890" + id % 10);
        membro.setSenha(new BCryptPasswordEncoder().encode("SenhaAntiga1!"));
        int idPapel = switch (papel) { case "ADMINISTRADOR" -> 1; case "COORDENADOR" -> 2; default -> 3; };
        membro.setPapel(new Papel(idPapel, papel, ""));
        membro.setUnidades(List.of(idsUnidades).stream().map(idUnidade -> {
            Unidade unidade = new Unidade();
            unidade.setId(idUnidade);
            return unidade;
        }).toList());
        return membro;
    }

    private ReativarMembroDTO dadosReativacao(Papel papel) {
        var dto = new ReativarMembroDTO();
        dto.setNomeCompleto("Monitor Revisado");
        dto.setEmail("revisado@teste.com");
        dto.setCpf("98765432100");
        dto.setIdPapel(papel.getId());
        dto.setIdsUnidades(List.of(1));
        dto.setSenha("NovaSenha1!");
        dto.setConfirmacaoSenha("NovaSenha1!");
        return dto;
    }

    private ResponseStatusException falha(org.assertj.core.api.ThrowableAssert.ThrowingCallable acao) {
        return (ResponseStatusException) catchThrowable(acao);
    }

    @Test
    void desativaMonitorInvalidaSessoesEPreservaDados() {
        Membro monitor = membro(20, "MONITOR", 1);
        when(membros.bloquearParaGestao(20)).thenReturn(List.of(monitor));
        when(membros.save(monitor)).thenReturn(monitor);

        var resposta = service.desativarMembro(20, coordenador);

        assertThat(resposta.isAtivo()).isFalse();
        assertThat(monitor.isEnabled()).isFalse();
        assertThat(monitor.getVersaoSessao()).isEqualTo(1);
        assertThat(monitor.getEmail()).isEqualTo("m20@teste.com");
        assertThat(monitor.getUnidades()).hasSize(1);
    }

    @Test
    void naoDesativaAPropriaConta() {
        assertThat(falha(() -> service.desativarMembro(10, coordenador)).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(membros);
    }

    @Test
    void coordenadorNaoDesativaMonitorDeOutraUnidadeNemColaboradorDeOutroPerfil() {
        Membro externo = membro(20, "MONITOR", 3);
        Membro outroCoordenador = membro(21, "COORDENADOR", 1);
        when(membros.bloquearParaGestao(20)).thenReturn(List.of(externo));
        when(membros.bloquearParaGestao(21)).thenReturn(List.of(outroCoordenador));

        assertThat(falha(() -> service.desativarMembro(20, coordenador)).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(falha(() -> service.desativarMembro(21, coordenador)).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(externo.isAtivo()).isTrue();
        verify(membros, never()).save(any());
    }

    @Test
    void desativarInativoRetornaConflito() {
        Membro monitor = membro(20, "MONITOR", 1);
        monitor.setAtivo(false);
        when(membros.bloquearParaGestao(20)).thenReturn(List.of(monitor));

        assertThat(falha(() -> service.desativarMembro(20, coordenador)).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(monitor.getVersaoSessao()).isZero();
    }

    @Test
    void impedeDesativarOUltimoAdministradorAtivo() {
        Membro unico = membro(2, "ADMINISTRADOR");
        when(membros.bloquearParaGestao(2)).thenReturn(List.of(unico));

        var erro = falha(() -> service.desativarMembro(2, admin));

        assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(unico.isAtivo()).isTrue();
    }

    @Test
    void desativaAdministradorQuandoHaOutroAtivo() {
        Membro alvo = membro(2, "ADMINISTRADOR");
        when(membros.bloquearParaGestao(2)).thenReturn(List.of(admin, alvo));
        when(membros.save(alvo)).thenReturn(alvo);

        assertThat(service.desativarMembro(2, admin).isAtivo()).isFalse();
    }

    @Test
    void reativaComDadosRevisadosENovaSenhaEmUmaUnicaOperacao() {
        Membro monitor = membro(20, "MONITOR", 1);
        monitor.setAtivo(false);
        monitor.invalidarSessoes();
        Papel papelMonitor = new Papel(3, "MONITOR", "");
        Unidade unidade = new Unidade();
        unidade.setId(1);
        when(membros.bloquearParaGestao(20)).thenReturn(List.of(monitor));
        when(papeis.findById(3)).thenReturn(Optional.of(papelMonitor));
        when(unidades.buscarUnidadesPorIds(List.of(1))).thenReturn(List.of(unidade));
        when(membros.saveAndFlush(monitor)).thenReturn(monitor);

        var resposta = service.reativarMembro(20, dadosReativacao(papelMonitor), coordenador);

        assertThat(resposta.isAtivo()).isTrue();
        assertThat(monitor.getNomeCompleto()).isEqualTo("Monitor Revisado");
        assertThat(monitor.getEmail()).isEqualTo("revisado@teste.com");
        assertThat(new BCryptPasswordEncoder().matches("NovaSenha1!", monitor.getSenha())).isTrue();
        assertThat(monitor.getVersaoSessao()).isEqualTo(2);
    }

    @Test
    void reativarExigeSenhaDiferenteDaAnteriorEConfirmacaoIgual() {
        Membro monitor = membro(20, "MONITOR", 1);
        monitor.setAtivo(false);
        Papel papelMonitor = new Papel(3, "MONITOR", "");
        when(membros.bloquearParaGestao(20)).thenReturn(List.of(monitor));
        when(papeis.findById(3)).thenReturn(Optional.of(papelMonitor));

        var repetida = dadosReativacao(papelMonitor);
        repetida.setSenha("SenhaAntiga1!");
        repetida.setConfirmacaoSenha("SenhaAntiga1!");
        var divergente = dadosReativacao(papelMonitor);
        divergente.setConfirmacaoSenha("Outra1!");

        assertThat(falha(() -> service.reativarMembro(20, repetida, coordenador)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(falha(() -> service.reativarMembro(20, divergente, coordenador)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(monitor.isAtivo()).isFalse();
        verify(membros, never()).saveAndFlush(any());
    }

    @Test
    void reativarAtivoRetornaConflitoECoordenadorNaoPromoveAoReativar() {
        Membro ativo = membro(20, "MONITOR", 1);
        Membro inativo = membro(21, "MONITOR", 1);
        inativo.setAtivo(false);
        when(membros.bloquearParaGestao(20)).thenReturn(List.of(ativo));
        when(membros.bloquearParaGestao(21)).thenReturn(List.of(inativo));
        Papel papelCoordenador = new Papel(2, "COORDENADOR", "");
        when(papeis.findById(2)).thenReturn(Optional.of(papelCoordenador));

        assertThat(falha(() -> service.reativarMembro(20, dadosReativacao(papelCoordenador), coordenador))
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(falha(() -> service.reativarMembro(21, dadosReativacao(papelCoordenador), coordenador))
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(inativo.isAtivo()).isFalse();
    }

    @Test
    void falhaDeDuplicidadeNaReativacaoMantemInativoEOrientaSobreCadastroInativo() {
        Membro monitor = membro(20, "MONITOR", 1);
        monitor.setAtivo(false);
        Membro outroInativo = membro(30, "MONITOR", 1);
        outroInativo.setAtivo(false);
        Papel papelMonitor = new Papel(3, "MONITOR", "");
        when(membros.bloquearParaGestao(20)).thenReturn(List.of(monitor));
        when(papeis.findById(3)).thenReturn(Optional.of(papelMonitor));
        when(membros.findByEmail("revisado@teste.com")).thenReturn(Optional.of(outroInativo));

        var erro = falha(() -> service.reativarMembro(20, dadosReativacao(papelMonitor), coordenador));

        assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(erro.getReason()).contains("inativo").contains("Reative");
        assertThat(monitor.isAtivo()).isFalse();
        verify(membros, never()).saveAndFlush(any());
    }

    @Test
    void violacaoDeUnicidadeNoBancoViraConflito() {
        Membro monitor = membro(20, "MONITOR", 1);
        monitor.setAtivo(false);
        Papel papelMonitor = new Papel(3, "MONITOR", "");
        Unidade unidade = new Unidade();
        unidade.setId(1);
        when(membros.bloquearParaGestao(20)).thenReturn(List.of(monitor));
        when(papeis.findById(3)).thenReturn(Optional.of(papelMonitor));
        when(unidades.buscarUnidadesPorIds(List.of(1))).thenReturn(List.of(unidade));
        when(membros.saveAndFlush(monitor)).thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThat(falha(() -> service.reativarMembro(20, dadosReativacao(papelMonitor), coordenador))
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void cadastroComEmailDeInativoOrientaReativacao() {
        Membro inativo = membro(30, "MONITOR", 1);
        inativo.setAtivo(false);
        when(membros.findById(10)).thenReturn(Optional.of(coordenador));
        when(papeis.findById(3)).thenReturn(Optional.of(new Papel(3, "MONITOR", "")));
        when(membros.findByEmail("novo@teste.com")).thenReturn(Optional.of(inativo));
        var dto = new CriarMembroDTO();
        dto.setIdPapel(3);
        dto.setEmail("novo@teste.com");
        dto.setCpf("11122233344");
        dto.setSenha("Senha123!");
        dto.setIdsUnidades(List.of(1));

        var erro = falha(() -> service.criarMembro(dto, coordenador));

        assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(erro.getReason()).contains("inativo");
    }

    @Test
    void filtroPorSituacaoUsaConsultaDeSituacaoEPreservaRestricaoDoCoordenador() {
        when(membros.findById(10)).thenReturn(Optional.of(coordenador));
        when(membros.listarMonitoresDasUnidadesPorSituacao(List.of(1, 2), null, null, null, null, true, pagina))
                .thenReturn(new PageImpl<>(List.of(membro(20, "MONITOR", 1)), pagina, 1));

        var resposta = service.getAllMembros(pagina, null, null, null, true, coordenador);

        assertThat(resposta.getContent()).extracting(dto -> dto.getId()).containsExactly(20);
        verify(membros, never()).listarMonitoresDasUnidades(any(), any(), any(), any(), any(), any());
    }
}
