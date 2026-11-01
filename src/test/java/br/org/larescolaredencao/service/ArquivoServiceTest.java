package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class ArquivoServiceTest {

    @Test
    void deveRejeitarArquivoAcimaDeDezMbAntesDeGravar() throws IOException {
        MockMultipartFile documento = new MockMultipartFile("arquivo", "documento.pdf", "application/pdf",
                new byte[10 * 1024 * 1024 + 1]);
        assertThatThrownBy(() -> arquivoService.salvarArquivo(documento, "documentos/", TipoArquivo.DOCUMENTO))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("10 MB");
        try (var arquivos = Files.list(pastaUploads)) {
            assertThat(arquivos.count()).isZero();
        }
    }

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

    @Test
    void deveGravarDocxSemAlteracao() throws IOException {
        Map<String, String> partes = Map.of(
                "[Content_Types].xml", """
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                    </Types>
                    """,
                "_rels/.rels", """
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                    </Relationships>
                    """,
                "word/document.xml", """
                    <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                      <w:body><w:p><w:r><w:t>Documento de teste: preservação de conteúdo e acentuação.</w:t></w:r></w:p></w:body>
                    </w:document>
                    """);
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(saida)) {
            for (var parte : partes.entrySet()) {
                zip.putNextEntry(new ZipEntry(parte.getKey()));
                zip.write(parte.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        byte[] docx = saida.toByteArray();
        MockMultipartFile documento = new MockMultipartFile("arquivo", "documento.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docx);
        String caminho = arquivoService.salvarArquivo(documento, "paginas/1/documentos/", TipoArquivo.DOCUMENTO);
        assertThat(caminho).endsWith("_documento.docx");
        assertThat(Files.readAllBytes(arquivoGravado(caminho))).isEqualTo(docx);
    }

    private Path arquivoGravado(String caminhoNoBanco) {
        return pastaUploads.resolve(caminhoNoBanco.substring("/uploads/".length()));
    }
}
