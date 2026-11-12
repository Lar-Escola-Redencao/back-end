package br.org.larescolaredencao.service;

import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Papel;
import br.org.larescolaredencao.model.Turma;
import br.org.larescolaredencao.model.Unidade;
import br.org.larescolaredencao.model.Usuario;
import br.org.larescolaredencao.model.enums.StatusMatricula;
import br.org.larescolaredencao.repository.MatriculaRepository;
import br.org.larescolaredencao.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FotoPerfilUsuarioTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private MatriculaRepository matriculaRepository;

    @Mock
    private ArquivoService arquivoService;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        Unidade unidade = new Unidade();
        unidade.setId(1);
        Turma turma = new Turma();
        turma.setId(1);
        turma.setUnidade(unidade);

        usuario = new Usuario();
        usuario.setId(7);
        usuario.setImagemPerfil("/uploads/usuarios/abc_foto.jpg");

        Matricula matricula = new Matricula();
        matricula.setUsuario(usuario);
        matricula.setTurma(turma);
        matricula.setStatus(StatusMatricula.ATIVO);
        matricula.setDataIngresso(LocalDateTime.now());

        lenient().when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario));
        lenient().when(matriculaRepository.findByUsuario(usuario)).thenReturn(List.of(matricula));
    }

    private Membro membro(String papel, Integer... unidades) {
        Membro membro = new Membro();
        membro.setPapel(new Papel(1, papel, ""));
        membro.setUnidades(java.util.Arrays.stream(unidades).map(id -> {
            Unidade unidade = new Unidade();
            unidade.setId(id);
            return unidade;
        }).toList());
        return membro;
    }

    @Test
    void monitorDaUnidadeCarregaFoto() {
        Resource recurso = new ByteArrayResource(new byte[] {1});
        when(arquivoService.carregarComoRecurso("/uploads/usuarios/abc_foto.jpg")).thenReturn(recurso);

        assertThat(usuarioService.carregarFotoPerfil(7, membro("MONITOR", 1))).isSameAs(recurso);
    }

    @Test
    void coordenadorDeOutraUnidadeNaoCarregaFoto() {
        assertThatThrownBy(() -> usuarioService.carregarFotoPerfil(7, membro("COORDENADOR", 2)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(arquivoService, never()).carregarComoRecurso(any());
    }

    @Test
    void usuarioSemFotoRetorna404() {
        usuario.setImagemPerfil(null);
        assertThatThrownBy(() -> usuarioService.carregarFotoPerfil(7, membro("ADMINISTRADOR")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void caminhoForaDaPastaDeFotosNaoEhServido() {
        usuario.setImagemPerfil("/uploads/usuarios/saude/exame.pdf");
        assertThatThrownBy(() -> usuarioService.carregarFotoPerfil(7, membro("ADMINISTRADOR")))
                .isInstanceOf(ResponseStatusException.class);

        usuario.setImagemPerfil("/uploads/usuarios/../paginas/x.jpg");
        assertThatThrownBy(() -> usuarioService.carregarFotoPerfil(7, membro("ADMINISTRADOR")))
                .isInstanceOf(ResponseStatusException.class);
        verify(arquivoService, never()).carregarComoRecurso(any());
    }

    @Test
    void respostaExpoeApenasRotaProtegida() {
        String url = FotoPerfilUsuario.urlProtegida(7, "/uploads/usuarios/abc_maria-silva.jpg");
        assertThat(url).startsWith("/usuarios/7/foto?v=").doesNotContain("maria").doesNotContain("uploads");
        assertThat(FotoPerfilUsuario.urlProtegida(7, null)).isNull();
        assertThat(FotoPerfilUsuario.urlProtegida(7, "/uploads/usuarios/outra.jpg")).isNotEqualTo(url);
    }
}
