package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import br.org.larescolaredencao.dto.MembroResponseDTO;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Papel;
import br.org.larescolaredencao.repository.MembroRepository;
import br.org.larescolaredencao.repository.PapelRepository;

@ExtendWith(MockitoExtension.class)
class MembroServiceTeste {

    @Mock
    private MembroRepository membroRepository;

    @Mock
    private PapelRepository papelRepository;

    @InjectMocks
    private MembroService membroService;

    private Pageable pageable;
    private Papel papel;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
        papel = new Papel(1, "VOLUNTARIO", "Voluntário da instituição");
    }

    @Test
    void deveListarTodosOsMembrosQuandoIdPapelNaoInformado() {
        Membro membro = new Membro(1, "Maria Souza", "maria@teste.com", "senha", "12345678900", "Rua A", "999999999", papel);

        Page<Membro> paginaEsperada = new PageImpl<>(List.of(membro), pageable, 1);
        when(membroRepository.findAll(pageable)).thenReturn(paginaEsperada);

        Page<MembroResponseDTO> resultado = membroService.getAllMembros(pageable, null);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getNomeCompleto()).isEqualTo("Maria Souza");
        verify(membroRepository).findAll(pageable);
        verify(membroRepository, never()).findByPapelId(anyInt(), any());
    }

    @Test
    void deveListarMembrosFiltrandoPorIdPapelQuandoInformado() {
        Membro membro = new Membro(2, "João Lima", "joao@teste.com", "senha", "98765432100", "Rua B", "888888888", papel);

        Page<Membro> paginaEsperada = new PageImpl<>(List.of(membro), pageable, 1);
        when(membroRepository.findByPapelId(1, pageable)).thenReturn(paginaEsperada);

        Page<MembroResponseDTO> resultado = membroService.getAllMembros(pageable, 1);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getIdPapel()).isEqualTo(1);
        verify(membroRepository).findByPapelId(1, pageable);
        verify(membroRepository, never()).findAll(pageable);
    }

    @Test
    void deveRetornarPaginaVaziaQuandoNaoHaMembros() {
        Page<Membro> paginaVazia = new PageImpl<>(List.of(), pageable, 0);
        when(membroRepository.findAll(pageable)).thenReturn(paginaVazia);

        Page<MembroResponseDTO> resultado = membroService.getAllMembros(pageable, null);

        assertThat(resultado.getContent()).isEmpty();
        assertThat(resultado.getTotalElements()).isZero();
    }
}
