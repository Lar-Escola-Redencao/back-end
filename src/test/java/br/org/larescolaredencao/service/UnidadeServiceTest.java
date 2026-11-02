package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import br.org.larescolaredencao.model.Unidade;
import br.org.larescolaredencao.repository.MatriculaRepository;
import br.org.larescolaredencao.repository.TurmaRepository;
import br.org.larescolaredencao.repository.UnidadeRepository;
import br.org.larescolaredencao.repository.MembroRepository;

@ExtendWith(MockitoExtension.class)
class UnidadeServiceTest {
    @Mock UnidadeRepository unidades;
    @Mock TurmaRepository turmas;
    @Mock MatriculaRepository matriculas;
    @Mock ArquivoService arquivos;
    @Mock MembroRepository membros;
    @InjectMocks UnidadeService service;
    Unidade unidade;

    @BeforeEach
    void preparar() {
        unidade = new Unidade(); unidade.setId(1); unidade.setImagem("/uploads/unidade.jpg");
        when(unidades.findById(1)).thenReturn(Optional.of(unidade));
    }

    @Test
    void explicaVinculoComMatriculasAtuaisOuAntigasSemApagarImagem() {
        when(matriculas.existsByTurmaUnidadeId(1)).thenReturn(true);
        assertThatThrownBy(() -> service.deletarUnidade(1))
                .isInstanceOfSatisfying(ResponseStatusException.class, erro -> {
                    assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(erro.getReason()).contains("matrículas vinculadas", "transferidos ou desligados");
                });
        verify(unidades, never()).delete(any()); verifyNoInteractions(arquivos);
    }

    @Test
    void explicaQuandoHaTurmasSemMatriculas() {
        when(turmas.existsByUnidadeId(1)).thenReturn(true);
        assertThatThrownBy(() -> service.deletarUnidade(1))
                .isInstanceOfSatisfying(ResponseStatusException.class, erro -> {
                    assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(erro.getReason()).contains("turmas vinculadas").doesNotContain("matrículas");
                });
        verify(unidades, never()).delete(any()); verifyNoInteractions(arquivos);
    }

    @Test
    void verificaExclusaoAntesDeApagarImagem() {
        service.deletarUnidade(1);
        var ordem = inOrder(unidades, arquivos);
        ordem.verify(unidades).delete(unidade); ordem.verify(unidades).flush();
        ordem.verify(arquivos).deletarArquivo("/uploads/unidade.jpg");
    }

    @Test
    void bloqueiaUnidadeComCoordenadoresOuMonitoresSemApagarImagem() {
        when(membros.existsByUnidadesIdAndPapelNomePapelNot(1, "ADMINISTRADOR")).thenReturn(true);
        assertThatThrownBy(() -> service.deletarUnidade(1))
                .isInstanceOfSatisfying(ResponseStatusException.class, erro -> {
                    assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(erro.getReason()).contains("colaboradores vinculados", "vínculos");
                });
        verify(unidades, never()).delete(any()); verifyNoInteractions(arquivos);
    }

    @Test
    void desconsideraAdministradoresNaVerificacaoDeVinculos() {
        service.deletarUnidade(1);
        verify(membros).existsByUnidadesIdAndPapelNomePapelNot(1, "ADMINISTRADOR");
        verify(unidades).delete(unidade);
    }

    @Test
    void preservaImagemEExplicaOutrosVinculosQuandoBancoRecusaExclusao() {
        doThrow(new DataIntegrityViolationException("fk_vinculo")).when(unidades).flush();
        assertThatThrownBy(() -> service.deletarUnidade(1))
                .isInstanceOfSatisfying(ResponseStatusException.class, erro -> {
                    assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(erro.getReason()).contains("registros vinculados");
                });
        verifyNoInteractions(arquivos);
    }
}
