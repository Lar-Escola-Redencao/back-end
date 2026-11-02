package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import br.org.larescolaredencao.dto.AtualizarParceiroDTO;
import br.org.larescolaredencao.dto.AtualizarRedeSocialDTO;
import br.org.larescolaredencao.model.Parceiro;
import br.org.larescolaredencao.model.RedeSocial;
import br.org.larescolaredencao.repository.ParceiroRepository;
import br.org.larescolaredencao.repository.RedeSocialRepository;
import br.org.larescolaredencao.repository.EventoRepository;
import br.org.larescolaredencao.repository.EventoRedeSocialRepository;

class SubstituicaoImagemTest {
    @TempDir Path uploads;
    ArquivoService arquivos;
    MockMultipartFile foto;
    MockMultipartFile invalida = new MockMultipartFile("logo", "invalida.png", "image/png", new byte[]{1, 2, 3});

    @BeforeEach
    void preparar() throws Exception {
        arquivos = new ArquivoService(new CompressorImagem());
        ReflectionTestUtils.setField(arquivos, "uploadDir", uploads + "/");
        foto = new MockMultipartFile("logo", "foto.jpg", "image/jpeg", CompressorImagemTest.jpeg(100, 100));
    }

    @Test
    void devePreservarLogoDoParceiroAoRejeitarNovaImagem() {
        var repo = mock(ParceiroRepository.class);
        var parceiro = new Parceiro();
        parceiro.setLogo(arquivos.salvarArquivo(foto, "parceiros/"));
        String antiga = parceiro.getLogo();
        when(repo.findById(1L)).thenReturn(Optional.of(parceiro));
        var dto = new AtualizarParceiroDTO(); dto.setLogo(invalida);
        assertThatThrownBy(() -> new ParceiroService(repo, arquivos, mock(EventoRepository.class)).atualizarParceiro(1L, dto))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(uploads.resolve(antiga.substring(9))).exists();
        assertThat(parceiro.getLogo()).isEqualTo(antiga);
        verify(repo, never()).save(any());
    }

    @Test
    void devePreservarIconeDaRedeSocialAoRejeitarNovaImagem() {
        var repo = mock(RedeSocialRepository.class);
        var rede = new RedeSocial();
        rede.setIcone(arquivos.salvarArquivo(foto, "redes-sociais/"));
        String antiga = rede.getIcone();
        when(repo.findById(1L)).thenReturn(Optional.of(rede));
        var dto = new AtualizarRedeSocialDTO(); dto.setIcone(invalida);
        assertThatThrownBy(() -> new RedeSocialService(repo, arquivos, mock(EventoRedeSocialRepository.class)).atualizarRedeSocial(1L, dto))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(uploads.resolve(antiga.substring(9))).exists();
        assertThat(rede.getIcone()).isEqualTo(antiga);
        verify(repo, never()).save(any());
    }

    @Test
    void deveRemoverLogoAntigaSomenteDepoisDeSalvarParceiro() {
        var repo = mock(ParceiroRepository.class);
        var parceiro = new Parceiro();
        parceiro.setLogo(arquivos.salvarArquivo(foto, "parceiros/"));
        String antiga = parceiro.getLogo();
        when(repo.findById(1L)).thenReturn(Optional.of(parceiro));
        when(repo.save(any())).thenAnswer(invocacao -> {
            assertThat(uploads.resolve(antiga.substring(9))).exists();
            assertThat(uploads.resolve(parceiro.getLogo().substring(9))).exists();
            return invocacao.getArgument(0);
        });
        var dto = new AtualizarParceiroDTO(); dto.setLogo(foto);
        new ParceiroService(repo, arquivos, mock(EventoRepository.class)).atualizarParceiro(1L, dto);
        assertThat(uploads.resolve(antiga.substring(9))).doesNotExist();
        assertThat(uploads.resolve(parceiro.getLogo().substring(9))).exists();
    }

    @Test
    void devePreservarLogoELimparNovoArquivoSeBancoFalhar() throws Exception {
        var repo = mock(ParceiroRepository.class);
        var parceiro = new Parceiro();
        parceiro.setLogo(arquivos.salvarArquivo(foto, "parceiros/"));
        String antiga = parceiro.getLogo();
        when(repo.findById(1L)).thenReturn(Optional.of(parceiro));
        when(repo.save(any())).thenThrow(new IllegalStateException("Banco indisponível"));
        var dto = new AtualizarParceiroDTO(); dto.setLogo(foto);
        assertThatThrownBy(() -> new ParceiroService(repo, arquivos, mock(EventoRepository.class)).atualizarParceiro(1L, dto))
                .isInstanceOf(IllegalStateException.class);
        assertThat(parceiro.getLogo()).isEqualTo(antiga);
        assertThat(uploads.resolve(antiga.substring(9))).exists();
        try (var criados = Files.list(uploads.resolve("parceiros"))) {
            assertThat(criados.count()).isEqualTo(1);
        }
    }
}
