package br.org.larescolaredencao.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import br.org.larescolaredencao.dto.CodigoPixResponseDTO;
import br.org.larescolaredencao.dto.ConfiguracaoPixResponseDTO;
import br.org.larescolaredencao.dto.GerarCodigoPixDTO;
import br.org.larescolaredencao.model.Secao;
import br.org.larescolaredencao.repository.PaginaRepository;
import br.org.larescolaredencao.repository.SecaoRepository;

@Service
public class PixService {

    private static final Long ID_PAGINA_PIX = 4L;
    private final PaginaRepository paginaRepository;
    private final SecaoRepository secaoRepository;
    private final String nomeRecebedor;
    private final String cidadeRecebedor;

    public PixService(PaginaRepository paginaRepository, SecaoRepository secaoRepository,
            @Value("${app.pix.nome-recebedor:Lar Escola Redencao}") String nomeRecebedor,
            @Value("${app.pix.cidade-recebedor:Araraquara}") String cidadeRecebedor) {
        this.paginaRepository = paginaRepository;
        this.secaoRepository = secaoRepository;
        this.nomeRecebedor = CodigoPix.normalizarNome(nomeRecebedor, 25);
        this.cidadeRecebedor = CodigoPix.normalizarNome(cidadeRecebedor, 15);
    }

    @Transactional(readOnly = true)
    public ConfiguracaoPixResponseDTO obterConfiguracao() {
        String chave = CodigoPix.normalizarChave(buscarPixAtivo().getConteudo());
        return new ConfiguracaoPixResponseDTO(chave, nomeRecebedor, cidadeRecebedor,
                List.of(new BigDecimal("10.00"), new BigDecimal("25.00"), new BigDecimal("50.00"), new BigDecimal("100.00")));
    }

    @Transactional(readOnly = true)
    public CodigoPixResponseDTO gerarCodigo(GerarCodigoPixDTO dto) {
        ConfiguracaoPixResponseDTO configuracao = obterConfiguracao();
        String identificador = "LER" + UUID.randomUUID().toString().replace("-", "").substring(0, 22);
        String payload = CodigoPix.gerar(configuracao.chave(), dto.valor(), nomeRecebedor, cidadeRecebedor, identificador);
        OffsetDateTime geradoEm = OffsetDateTime.now(ZoneOffset.UTC);
        return new CodigoPixResponseDTO(identificador, dto.valor().setScale(2), configuracao.chave(), nomeRecebedor,
                cidadeRecebedor, payload, gerarImagem(payload), geradoEm, geradoEm.plusMinutes(15));
    }

    private Secao buscarPixAtivo() {
        paginaRepository.findById(ID_PAGINA_PIX).filter(pagina -> Boolean.TRUE.equals(pagina.getAtivo()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doações via Pix indisponíveis."));
        return secaoRepository.findByPaginaIdOrderByGrupoAscOrdemAsc(ID_PAGINA_PIX).stream()
                .filter(secao -> "pix".equals(secao.getGrupo()) && Boolean.TRUE.equals(secao.getAtivo()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doações via Pix indisponíveis."));
    }

    private String gerarImagem(String payload) {
        try {
            BitMatrix matriz = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 360, 360,
                    Map.of(EncodeHintType.MARGIN, 4, EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M));
            BufferedImage imagem = new BufferedImage(matriz.getWidth(), matriz.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < matriz.getWidth(); x++) {
                for (int y = 0; y < matriz.getHeight(); y++) {
                    imagem.setRGB(x, y, matriz.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            ImageIO.write(imagem, "png", saida);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(saida.toByteArray());
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Não foi possível gerar o QR Code Pix.", e);
        }
    }
}
