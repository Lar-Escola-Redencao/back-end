package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.org.larescolaredencao.dto.CriarDiretoriaDTO;
import br.org.larescolaredencao.dto.CriarParceiroDTO;
import br.org.larescolaredencao.model.Parceiro;
import br.org.larescolaredencao.repository.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class CadastroSemImagemTest {
    @Test
    void aceitaDtoEEntidadeSemImagem() {
        var parceiro = new CriarParceiroDTO();
        parceiro.setNome("Parceiro teste");
        var diretoria = new CriarDiretoriaDTO();
        diretoria.setNome("Maria Silva");
        diretoria.setCargo("Presidente");
        var entidade = new Parceiro();
        entidade.setNome(parceiro.getNome());
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(parceiro)).isEmpty();
            assertThat(validator.validate(diretoria)).isEmpty();
            assertThat(validator.validate(entidade)).isEmpty();
        }
    }

    @Test
    void criaParceiroSemEnviarArquivoAoArmazenamento() {
        var repository = mock(ParceiroRepository.class);
        var arquivos = mock(ArquivoService.class);
        var service = new ParceiroService(repository, arquivos, mock(EventoRepository.class));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var dto = new CriarParceiroDTO();
        dto.setNome("Parceiro teste");
        assertThat(service.criarParceiro(dto).getLogo()).isNull();
        dto.setLogo(new MockMultipartFile("logo", new byte[0]));
        assertThat(service.criarParceiro(dto).getLogo()).isNull();
        verifyNoInteractions(arquivos);
    }

    @Test
    void criaDiretoriaSemEnviarArquivoAoArmazenamento() {
        var repository = mock(DiretoriaRepository.class);
        var arquivos = mock(ArquivoService.class);
        var service = new DiretoriaService(repository, arquivos);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var dto = new CriarDiretoriaDTO();
        dto.setNome("Maria Silva");
        dto.setCargo("Presidente");
        assertThat(service.criar(dto).getFoto()).isNull();
        dto.setFoto(new MockMultipartFile("foto", new byte[0]));
        assertThat(service.criar(dto).getFoto()).isNull();
        verifyNoInteractions(arquivos);
    }
}
