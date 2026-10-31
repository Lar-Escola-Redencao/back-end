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
    private static final String GRUPO_PIX = "pix";
    private static final int PRAZO_SESSAO_MINUTOS = 15;
    private static final int TAMANHO_QR_CODE = 360;
    private static final int COR_PRETA = 0x000000;
    private static final int COR_BRANCA = 0xFFFFFF;
    private static final List<BigDecimal> VALORES_SUGERIDOS = List.of(
            new BigDecimal("10.00"), new BigDecimal("25.00"),
            new BigDecimal("50.00"), new BigDecimal("100.00"));

    private final PaginaRepository paginaRepository;
    private final SecaoRepository secaoRepository;
    private final String nomeRecebedorPadrao;
    private final String cidadeRecebedor;

    public PixService(PaginaRepository paginaRepository, SecaoRepository secaoRepository,
            @Value("${app.pix.nome-recebedor:Lar Escola Redencao}") String nomeRecebedor,
            @Value("${app.pix.cidade-recebedor:Araraquara}") String cidadeRecebedor) {
        this.paginaRepository = paginaRepository;
        this.secaoRepository = secaoRepository;
        CodigoPix.normalizarNome(nomeRecebedor, 25);
        this.nomeRecebedorPadrao = nomeRecebedor.trim();
        this.cidadeRecebedor = CodigoPix.normalizarNome(cidadeRecebedor, 15);
    }

    @Transactional(readOnly = true)
    public ConfiguracaoPixResponseDTO obterConfiguracao() {
        Secao secao = buscarPixAtivo();
        String chave = CodigoPix.normalizarChave(secao.getConteudo());
        String recebedor = obterNomeCadastrado(secao);
        return new ConfiguracaoPixResponseDTO(chave, recebedor, cidadeRecebedor, VALORES_SUGERIDOS);
    }

    @Transactional(readOnly = true)
    public CodigoPixResponseDTO gerarCodigo(GerarCodigoPixDTO dto) {
        ConfiguracaoPixResponseDTO configuracao = obterConfiguracao();
        String identificador = gerarIdentificador();
        String nomeNoCodigo = obterNomeParaCodigo(configuracao);
        String payload = CodigoPix.gerar(configuracao.chave(), dto.valor(), nomeNoCodigo, cidadeRecebedor, identificador);
        OffsetDateTime geradoEm = OffsetDateTime.now(ZoneOffset.UTC);
        String imagem = gerarImagem(payload);
        return new CodigoPixResponseDTO(
                identificador, dto.valor().setScale(2), configuracao.chave(), configuracao.nomeRecebedor(),
                cidadeRecebedor, payload, imagem, geradoEm, geradoEm.plusMinutes(PRAZO_SESSAO_MINUTOS));
    }

    private String obterNomeCadastrado(Secao secao) {
        String titulo = secao.getTitulo();
        return titulo == null || titulo.isBlank() ? null : titulo.trim();
    }

    private String obterNomeParaCodigo(ConfiguracaoPixResponseDTO configuracao) {
        return configuracao.nomeRecebedor() == null ? nomeRecebedorPadrao : configuracao.nomeRecebedor();
    }

    private String gerarIdentificador() {
        String sufixo = UUID.randomUUID().toString().replace("-", "").substring(0, 22);
        return "LER" + sufixo;
    }

    private Secao buscarPixAtivo() {
        paginaRepository.findById(ID_PAGINA_PIX)
                .filter(pagina -> Boolean.TRUE.equals(pagina.getAtivo()))
                .orElseThrow(this::pixIndisponivel);
        return secaoRepository.findByPaginaIdOrderByGrupoAscOrdemAsc(ID_PAGINA_PIX).stream()
                .filter(secao -> GRUPO_PIX.equals(secao.getGrupo()) && Boolean.TRUE.equals(secao.getAtivo()))
                .findFirst()
                .orElseThrow(this::pixIndisponivel);
    }

    private ResponseStatusException pixIndisponivel() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Doações via Pix indisponíveis.");
    }

    private String gerarImagem(String payload) {
        try {
            BitMatrix matriz = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, TAMANHO_QR_CODE, TAMANHO_QR_CODE,
                    Map.of(EncodeHintType.MARGIN, 4, EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M));
            BufferedImage imagem = converterMatrizEmImagem(matriz);
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            ImageIO.write(imagem, "png", saida);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(saida.toByteArray());
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Não foi possível gerar o QR Code Pix.", e);
        }
    }

    private BufferedImage converterMatrizEmImagem(BitMatrix matriz) {
        int largura = matriz.getWidth();
        int altura = matriz.getHeight();
        BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                imagem.setRGB(x, y, matriz.get(x, y) ? COR_PRETA : COR_BRANCA);
            }
        }
        return imagem;
    }
}
