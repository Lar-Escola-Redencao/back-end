package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.service.CompressorImagem.ImagemComprimida;

class CompressorImagemTest {

    private final CompressorImagem compressor = new CompressorImagem();

    @Test
    void deveReduzirFotoGrandeAoLadoMaximoDoTipoFoto() throws IOException {
        byte[] original = jpeg(4000, 3000);

        ImagemComprimida resultado = compressor.comprimir(original, TipoArquivo.FOTO);

        BufferedImage imagem = ler(resultado.conteudo());
        assertThat(resultado.extensao()).isEqualTo("jpg");
        assertThat(imagem.getWidth()).isEqualTo(1920);
        assertThat(imagem.getHeight()).isEqualTo(1440);
        assertThat(resultado.conteudo().length).isLessThan(original.length / 2);
    }

    @Test
    void deveUsarLimiteMaiorParaDocumentoDeSaude() throws IOException {
        ImagemComprimida resultado = compressor.comprimir(jpeg(4000, 3000), TipoArquivo.SAUDE);

        BufferedImage imagem = ler(resultado.conteudo());
        assertThat(imagem.getWidth()).isEqualTo(2560);
        assertThat(imagem.getHeight()).isEqualTo(1920);
    }

    @Test
    void deveManterProporcaoDeFotoNaVertical() throws IOException {
        BufferedImage imagem = ler(compressor.comprimir(jpeg(3000, 4000), TipoArquivo.FOTO).conteudo());

        assertThat(imagem.getWidth()).isEqualTo(1440);
        assertThat(imagem.getHeight()).isEqualTo(1920);
    }

    @Test
    void naoDeveAmpliarImagemMenorQueOLimite() throws IOException {
        BufferedImage imagem = ler(compressor.comprimir(jpeg(800, 600), TipoArquivo.FOTO).conteudo());

        assertThat(imagem.getWidth()).isEqualTo(800);
        assertThat(imagem.getHeight()).isEqualTo(600);
    }

    @Test
    void deveManterPngComTransparencia() throws IOException {
        BufferedImage logo = new BufferedImage(3000, 1000, BufferedImage.TYPE_INT_ARGB);
        logo.setRGB(2999, 999, 0xFF336699);

        ImagemComprimida resultado = compressor.comprimir(codificar(logo, "png"), TipoArquivo.FOTO);

        BufferedImage imagem = ler(resultado.conteudo());
        assertThat(resultado.extensao()).isEqualTo("png");
        assertThat(imagem.getWidth()).isEqualTo(1920);
        assertThat(imagem.getHeight()).isEqualTo(640);
        assertThat(imagem.getRGB(0, 0) >>> 24).isZero();
    }

    @Test
    void deveConverterFotoSalvaEmPngParaJpeg() throws IOException {
        byte[] original = codificar(ler(jpeg(2400, 1600)), "png");

        ImagemComprimida resultado = compressor.comprimir(original, TipoArquivo.FOTO);

        assertThat(resultado.extensao()).isEqualTo("jpg");
        assertThat(ler(resultado.conteudo()).getWidth()).isEqualTo(1920);
        assertThat(resultado.conteudo().length).isLessThan(original.length / 2);
    }

    @Test
    void deveManterLogoOpacoEmPng() throws IOException {
        BufferedImage logo = new BufferedImage(3000, 1000, BufferedImage.TYPE_INT_RGB);
        Graphics2D grafico = logo.createGraphics();
        grafico.setColor(Color.WHITE);
        grafico.fillRect(0, 0, 3000, 1000);
        grafico.setColor(new Color(0x336699));
        grafico.fillRect(500, 200, 2000, 600);
        grafico.dispose();

        ImagemComprimida resultado = compressor.comprimir(codificar(logo, "png"), TipoArquivo.FOTO);

        assertThat(resultado.extensao()).isEqualTo("png");
        assertThat(ler(resultado.conteudo()).getWidth()).isEqualTo(1920);
    }

    @Test
    void deveConverterWebpOpacoParaJpeg() throws IOException {
        byte[] original = fixture("opaca-grande.webp");

        ImagemComprimida resultado = compressor.comprimir(original, TipoArquivo.FOTO);

        BufferedImage imagem = ler(resultado.conteudo());
        assertThat(resultado.extensao()).isEqualTo("jpg");
        assertThat(imagem.getWidth()).isEqualTo(1920);
        assertThat(imagem.getHeight()).isEqualTo(480);
        assertThat(resultado.conteudo().length).isLessThan(original.length);
    }

    @Test
    void deveManterWebpQuandoRecompressaoNaoReduz() throws IOException {
        byte[] original = fixture("transparente.webp");

        ImagemComprimida resultado = compressor.comprimir(original, TipoArquivo.FOTO);

        assertThat(resultado.extensao()).isEqualTo("webp");
        assertThat(resultado.conteudo()).isEqualTo(original);
    }

    @Test
    void deveAplicarOrientacaoExifERemoverMetadados() throws IOException {
        // Fixture gravada em 300x200 com Orientation=6 (exibida em pé) e coordenadas GPS.
        byte[] original = fixture("orientacao-6-com-gps.jpg");
        assertThat(contemExif(original)).isTrue();

        ImagemComprimida resultado = compressor.comprimir(original, TipoArquivo.FOTO);

        BufferedImage imagem = ler(resultado.conteudo());
        assertThat(imagem.getWidth()).isEqualTo(200);
        assertThat(imagem.getHeight()).isEqualTo(300);
        assertThat(contemExif(resultado.conteudo())).isFalse();
    }

    @Test
    void deveRecusarArquivoQueNaoEImagem() {
        byte[] falso = "%PDF-1.4 conteudo qualquer".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> compressor.comprimir(falso, TipoArquivo.FOTO))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void deveRecusarImagemComResolucaoExcessiva() throws IOException {
        // 1 bit por pixel: 56 megapixels em poucos MB de memória e poucos KB de PNG.
        byte[] enorme = codificar(new BufferedImage(8000, 7000, BufferedImage.TYPE_BYTE_BINARY), "png");

        assertThatThrownBy(() -> compressor.comprimir(enorme, TipoArquivo.FOTO))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Resolução da imagem muito alta");
    }

    /** Gera um JPEG com ruído, para que o tamanho se aproxime ao de uma foto real. */
    static byte[] jpeg(int largura, int altura) throws IOException {
        BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Random aleatorio = new Random(42);
        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                int ruido = aleatorio.nextInt(64);
                imagem.setRGB(x, y, ((x * 255 / largura) << 16) | ((y * 255 / altura) << 8) | (128 + ruido));
            }
        }
        return codificar(imagem, "jpg");
    }

    static byte[] fixture(String nome) throws IOException {
        try (InputStream entrada = CompressorImagemTest.class.getResourceAsStream("/imagens/" + nome)) {
            return entrada.readAllBytes();
        }
    }

    static BufferedImage ler(byte[] conteudo) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(conteudo));
    }

    private static byte[] codificar(BufferedImage imagem, String formato) throws IOException {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, formato, saida);
        return saida.toByteArray();
    }

    private static boolean contemExif(byte[] conteudo) {
        return new String(conteudo, StandardCharsets.ISO_8859_1).contains("Exif\0\0");
    }
}
