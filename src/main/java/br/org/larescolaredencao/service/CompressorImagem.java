package br.org.larescolaredencao.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import net.coobird.thumbnailator.Thumbnails;

/**
 * Reduz e recomprime imagens antes de gravá-las.
 *
 * A recodificação também descarta os metadados EXIF (GPS, modelo do aparelho)
 * e garante que o conteúdo é de fato uma imagem, não apenas o MIME declarado.
 */
@Component
public class CompressorImagem {

    private static final Logger logger = LoggerFactory.getLogger(CompressorImagem.class);

    // Acima disso a imagem decodificada ocuparia centenas de MB de memória.
    private static final long MAXIMO_PIXELS = 50_000_000L;

    public record ImagemComprimida(byte[] conteudo, String extensao) {}

    private record Cabecalho(String formato, int largura, int altura) {}

    public CompressorImagem() {
        // Registra os leitores do TwelveMonkeys (WebP, JPEG CMYK) também no jar executável do Spring Boot.
        ImageIO.scanForPlugins();
    }

    public ImagemComprimida comprimir(byte[] original, TipoArquivo tipo) {
        Cabecalho cabecalho = lerCabecalho(original);
        if ((long) cabecalho.largura() * cabecalho.altura() > MAXIMO_PIXELS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resolução da imagem muito alta.");
        }

        boolean redimensionar = Math.max(cabecalho.largura(), cabecalho.altura()) > tipo.getDimensaoMaxima();
        BufferedImage imagem = decodificar(original, redimensionar ? tipo.getDimensaoMaxima() : 0);

        try {
            ImagemComprimida resultado;
            if (temTransparencia(imagem)) {
                resultado = new ImagemComprimida(codificarPng(imagem), "png");
            } else {
                resultado = new ImagemComprimida(codificarJpeg(paraRgb(imagem), tipo.getQualidadeJpeg()), "jpg");
                // Logos e capturas de tela ficam menores (e nítidos) em PNG; fotos salvas como PNG viram JPEG.
                if ("png".equals(cabecalho.formato())) {
                    byte[] png = codificarPng(imagem);
                    if (png.length < resultado.conteudo().length) {
                        resultado = new ImagemComprimida(png, "png");
                    }
                }
            }

            // JPEG é sempre regravado para descartar o EXIF; PNG e WebP nunca ficam maiores que o enviado.
            boolean formatoAceitoComoEsta = "png".equals(cabecalho.formato()) || "webp".equals(cabecalho.formato());
            if (formatoAceitoComoEsta && resultado.conteudo().length >= original.length) {
                resultado = new ImagemComprimida(original, cabecalho.formato());
            }

            logger.debug("Imagem comprimida: {} KB ({}x{} {}) -> {} KB ({})",
                    original.length / 1024, cabecalho.largura(), cabecalho.altura(), cabecalho.formato(),
                    resultado.conteudo().length / 1024, resultado.extensao());
            return resultado;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao comprimir a imagem.", e);
        }
    }

    private Cabecalho lerCabecalho(byte[] original) {
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(original))) {
            if (entrada == null) {
                throw imagemInvalida();
            }
            Iterator<ImageReader> leitores = ImageIO.getImageReaders(entrada);
            if (!leitores.hasNext()) {
                throw imagemInvalida();
            }
            ImageReader leitor = leitores.next();
            try {
                leitor.setInput(entrada, true, true);
                return new Cabecalho(leitor.getFormatName().toLowerCase(Locale.ROOT),
                        leitor.getWidth(0), leitor.getHeight(0));
            } finally {
                leitor.dispose();
            }
        } catch (IOException e) {
            throw imagemInvalida();
        }
    }

    /** Decodifica aplicando a orientação EXIF; com {@code dimensaoMaxima} > 0, reduz mantendo a proporção. */
    private BufferedImage decodificar(byte[] original, int dimensaoMaxima) {
        try {
            Thumbnails.Builder<?> construtor = Thumbnails.of(new ByteArrayInputStream(original));
            return dimensaoMaxima > 0
                    ? construtor.size(dimensaoMaxima, dimensaoMaxima).asBufferedImage()
                    : construtor.scale(1.0).asBufferedImage();
        } catch (IOException | IllegalArgumentException e) {
            throw imagemInvalida();
        }
    }

    private boolean temTransparencia(BufferedImage imagem) {
        if (!imagem.getColorModel().hasAlpha()) {
            return false;
        }
        for (int y = 0; y < imagem.getHeight(); y++) {
            for (int x = 0; x < imagem.getWidth(); x++) {
                if ((imagem.getRGB(x, y) >>> 24) != 0xFF) {
                    return true;
                }
            }
        }
        return false;
    }

    private BufferedImage paraRgb(BufferedImage imagem) {
        if (imagem.getType() == BufferedImage.TYPE_INT_RGB || imagem.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            return imagem;
        }
        BufferedImage rgb = new BufferedImage(imagem.getWidth(), imagem.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D grafico = rgb.createGraphics();
        grafico.drawImage(imagem, 0, 0, Color.WHITE, null);
        grafico.dispose();
        return rgb;
    }

    private byte[] codificarJpeg(BufferedImage imagem, float qualidade) throws IOException {
        ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam parametros = escritor.getDefaultWriteParam();
        parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        parametros.setCompressionQuality(qualidade);

        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        try (ImageOutputStream destino = ImageIO.createImageOutputStream(saida)) {
            escritor.setOutput(destino);
            escritor.write(null, new IIOImage(imagem, null, null), parametros);
        } finally {
            escritor.dispose();
        }
        return saida.toByteArray();
    }

    private byte[] codificarPng(BufferedImage imagem) throws IOException {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, "png", saida);
        return saida.toByteArray();
    }

    private ResponseStatusException imagemInvalida() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível processar a imagem enviada.");
    }
}
