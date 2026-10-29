package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class ArquivoServiceTest {

    @TempDir
    Path pastaUploads;

    private ArquivoService arquivoService;

    @BeforeEach
    void setUp() {
        arquivoService = new ArquivoService(new CompressorImagem());
        ReflectionTestUtils.setField(arquivoService, "uploadDir", pastaUploads + "/");
    }

    @Test
    void deveGravarFotoCompactada() throws IOException {
        byte[] original = CompressorImagemTest.jpeg(4000, 3000);
        MockMultipartFile foto = new MockMultipartFile("foto", "foto.jpeg", "image/jpeg", original);

        String caminho = arquivoService.salvarArquivo(foto, "diretoria/", TipoArquivo.FOTO);

        assertThat(caminho).startsWith("/uploads/diretoria/").endsWith("_foto.jpg");
        byte[] gravado = Files.readAllBytes(arquivoGravado(caminho));
        assertThat(gravado.length).isLessThan(original.length);
        BufferedImage imagem = CompressorImagemTest.ler(gravado);
        assertThat(imagem.getWidth()).isEqualTo(1920);
    }

    @Test
    void deveTrocarExtensaoQuandoWebpViraJpeg() throws IOException {
        MockMultipartFile foto = new MockMultipartFile("imagem", "banner.webp", "image/webp",
                CompressorImagemTest.fixture("opaca-grande.webp"));

        String caminho = arquivoService.salvarArquivo(foto, "eventos/", TipoArquivo.FOTO);

        assertThat(caminho).endsWith("_banner.jpg");
        assertThat(CompressorImagemTest.ler(Files.readAllBytes(arquivoGravado(caminho)))).isNotNull();
    }

    @Test
    void deveGravarDocumentoSemAlteracao() throws IOException {
        byte[] pdf = "%PDF-1.4 relatorio".getBytes();
        MockMultipartFile documento = new MockMultipartFile("arquivo", "relatorio.pdf", "application/pdf", pdf);

        String caminho = arquivoService.salvarArquivo(documento, "paginas/1/documentos/", TipoArquivo.DOCUMENTO);

        assertThat(caminho).endsWith("_relatorio.pdf");
        assertThat(Files.readAllBytes(arquivoGravado(caminho))).isEqualTo(pdf);
    }

    @Test
    void deveGravarPdfDeSaudeSemAlteracao() throws IOException {
        byte[] pdf = "%PDF-1.4 laudo".getBytes();
        MockMultipartFile laudo = new MockMultipartFile("arquivo", "laudo.pdf", "application/pdf", pdf);

        String caminho = arquivoService.salvarArquivo(laudo, "usuarios/saude/", TipoArquivo.SAUDE);

        assertThat(Files.readAllBytes(arquivoGravado(caminho))).isEqualTo(pdf);
    }

    private Path arquivoGravado(String caminhoNoBanco) {
        return pastaUploads.resolve(caminhoNoBanco.substring("/uploads/".length()));
    }
}
