package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import br.org.larescolaredencao.dto.GerarCodigoPixDTO;
import br.org.larescolaredencao.model.Pagina;
import br.org.larescolaredencao.model.Secao;
import br.org.larescolaredencao.repository.PaginaRepository;
import br.org.larescolaredencao.repository.SecaoRepository;

@ExtendWith(MockitoExtension.class)
class PixServiceTest {
    @Mock
    private PaginaRepository paginaRepository;
    @Mock
    private SecaoRepository secaoRepository;
    private PixService service;
    private Pagina pagina;

    @BeforeEach
    void preparar() {
        service = new PixService(paginaRepository, secaoRepository, "Lar Escola Redenção", "Araraquara");
        pagina = new Pagina();
        pagina.setAtivo(true);
        when(paginaRepository.findById(4L)).thenReturn(Optional.of(pagina));
    }

    @Test
    void imagemGeradaDeveDecodificarExatamenteParaOCopiaEColaSemGravarPagamento() throws Exception {
        Secao secao = new Secao();
        secao.setGrupo("pix");
        secao.setAtivo(true);
        secao.setConteudo("doacoes@example.org");
        when(secaoRepository.findByPaginaIdOrderByGrupoAscOrdemAsc(4L)).thenReturn(List.of(secao));

        var codigo = service.gerarCodigo(new GerarCodigoPixDTO(new BigDecimal("25.50")));
        var imagem = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(codigo.qrCode().split(",", 2)[1])));
        int largura = imagem.getWidth();
        int altura = imagem.getHeight();
        var fonte = new RGBLuminanceSource(largura, altura, imagem.getRGB(0, 0, largura, altura, null, 0, largura));
        var decodificado = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(fonte)));
        assertThat(decodificado.getText()).isEqualTo(codigo.copiaECola());
        assertThat(codigo.identificador()).hasSize(25);
        assertThat(codigo.valor()).isEqualByComparingTo("25.50");
        assertThat(codigo.expiraEm()).isEqualTo(codigo.geradoEm().plusMinutes(15));
        verify(secaoRepository, never()).save(any());
        assertThat(service.gerarCodigo(new GerarCodigoPixDTO(new BigDecimal("25.50")))).isNotEqualTo(codigo);
    }

    @Test
    void usaRecebedorDoCmsNaExibicaoENormalizaSomenteNoPayload() {
        Secao secao = new Secao();
        secao.setGrupo("pix");
        secao.setAtivo(true);
        secao.setTitulo("Instituto Esperança");
        secao.setConteudo("doacoes@example.org");
        when(secaoRepository.findByPaginaIdOrderByGrupoAscOrdemAsc(4L)).thenReturn(List.of(secao));
        assertThat(service.obterConfiguracao().nomeRecebedor()).isEqualTo("Instituto Esperança");
        var codigo = service.gerarCodigo(new GerarCodigoPixDTO(BigDecimal.TEN));
        assertThat(codigo.nomeRecebedor()).isEqualTo("Instituto Esperança");
        assertThat(codigo.copiaECola()).contains("INSTITUTO ESPERANCA").doesNotContain("LAR ESCOLA REDENCAO");
    }

    @Test
    void omiteRecebedorNaExibicaoQuandoSecaoNaoPossuiTitulo() {
        Secao secao = new Secao();
        secao.setGrupo("pix");
        secao.setAtivo(true);
        secao.setConteudo("doacoes@example.org");
        when(secaoRepository.findByPaginaIdOrderByGrupoAscOrdemAsc(4L)).thenReturn(List.of(secao));
        assertThat(service.obterConfiguracao().nomeRecebedor()).isNull();
        var codigo = service.gerarCodigo(new GerarCodigoPixDTO(BigDecimal.TEN));
        assertThat(codigo.nomeRecebedor()).isNull();
        assertThat(codigo.copiaECola()).contains("LAR ESCOLA REDENCAO");
    }

    @Test
    void paginaInativaNaoDeveGerarCodigo() {
        pagina.setAtivo(false);
        assertThatThrownBy(() -> service.gerarCodigo(new GerarCodigoPixDTO(BigDecimal.TEN)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
    }

    @Test
    void secaoInativaNaoDeveGerarCodigo() {
        Secao secao = new Secao();
        secao.setGrupo("pix");
        secao.setAtivo(false);
        when(secaoRepository.findByPaginaIdOrderByGrupoAscOrdemAsc(4L)).thenReturn(List.of(secao));
        assertThatThrownBy(() -> service.obterConfiguracao()).isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
    }
}
