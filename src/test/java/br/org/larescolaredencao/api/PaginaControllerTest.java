package br.org.larescolaredencao.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.AtualizarSecaoDTO;
import br.org.larescolaredencao.dto.CriarSecaoDTO;
import br.org.larescolaredencao.model.Secao;
import br.org.larescolaredencao.service.ArquivoService;
import br.org.larescolaredencao.service.PaginaService;

@ExtendWith(MockitoExtension.class)
class PaginaControllerTest {

    @Mock
    private PaginaService paginaService;

    @Mock
    private ArquivoService arquivoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PaginaController(paginaService, arquivoService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void putDeveAceitarMultipartFormDataComArquivo() throws Exception {
        Secao secao = new Secao();
        secao.setId(10L);
        secao.setGrupo("pix");
        secao.setConteudo("email@larredencao.org.br");
        secao.setImagem("/uploads/paginas/4/imagens/qr-novo.png");
        when(paginaService.atualizarSecao(eq(4L), eq(10L), any(AtualizarSecaoDTO.class))).thenReturn(secao);

        mockMvc.perform(multipart(HttpMethod.PUT, "/paginas/4/secoes/10")
                        .file(new MockMultipartFile("imagem", "qr-novo.png", "image/png", new byte[] {1, 2, 3}))
                        .param("grupo", "pix")
                        .param("conteudo", "email@larredencao.org.br"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").value("email@larredencao.org.br"))
                .andExpect(jsonPath("$.imagem").value("/uploads/paginas/4/imagens/qr-novo.png"));

        ArgumentCaptor<AtualizarSecaoDTO> dto = ArgumentCaptor.forClass(AtualizarSecaoDTO.class);
        verify(paginaService).atualizarSecao(eq(4L), eq(10L), dto.capture());
        assertThat(dto.getValue().getGrupo()).isEqualTo("pix");
        assertThat(dto.getValue().getConteudo()).isEqualTo("email@larredencao.org.br");
        assertThat(dto.getValue().getImagem().getOriginalFilename()).isEqualTo("qr-novo.png");
    }

    @Test
    void tituloVazioNaoEhBarradoNoBindingPoisOGrupoDecideSeUsaTitulo() throws Exception {
        when(paginaService.atualizarSecao(eq(4L), eq(10L), any(AtualizarSecaoDTO.class))).thenReturn(new Secao());
        when(paginaService.criarSecao(eq(4L), any(CriarSecaoDTO.class))).thenReturn(new Secao());

        mockMvc.perform(multipart(HttpMethod.PUT, "/paginas/4/secoes/10")
                        .param("grupo", "pix")
                        .param("titulo", "")
                        .param("conteudo", "email@larredencao.org.br"))
                .andExpect(status().isOk());

        mockMvc.perform(multipart("/paginas/4/secoes")
                        .param("grupo", "pix")
                        .param("titulo", "ab")
                        .param("conteudo", "email@larredencao.org.br"))
                .andExpect(status().isCreated());
    }

    @Test
    void postDeveAceitarMultipartFormDataERetornar201() throws Exception {
        Secao secao = new Secao();
        secao.setId(1L);
        secao.setGrupo("produtos");
        secao.setTitulo("Folder A3");
        when(paginaService.criarSecao(eq(3L), any(CriarSecaoDTO.class))).thenReturn(secao);

        mockMvc.perform(multipart("/paginas/3/secoes")
                        .file(new MockMultipartFile("imagem", "folder.png", "image/png", new byte[] {1}))
                        .param("grupo", "produtos")
                        .param("titulo", "Folder A3"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Folder A3"));

        ArgumentCaptor<CriarSecaoDTO> dto = ArgumentCaptor.forClass(CriarSecaoDTO.class);
        verify(paginaService).criarSecao(eq(3L), dto.capture());
        assertThat(dto.getValue().getImagem().getOriginalFilename()).isEqualTo("folder.png");
    }

    @Test
    void errosDeRegraDevemVoltarComStatusEMensagem() throws Exception {
        when(paginaService.criarSecao(eq(4L), any(CriarSecaoDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "O grupo 'telefone' não é permitido na página 4."));
        when(paginaService.atualizarSecao(eq(3L), eq(99L), any(AtualizarSecaoDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Seção não encontrada nesta página."));

        mockMvc.perform(multipart("/paginas/4/secoes").param("grupo", "telefone").param("titulo", "16999999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O grupo 'telefone' não é permitido na página 4."));

        mockMvc.perform(multipart(HttpMethod.PUT, "/paginas/3/secoes/99").param("grupo", "telefone"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Seção não encontrada nesta página."));
    }
}
