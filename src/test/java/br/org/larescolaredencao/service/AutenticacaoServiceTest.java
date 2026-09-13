package br.org.larescolaredencao.service;

import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.repository.MembroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTest {

    @Mock
    private MembroRepository repository;

    @InjectMocks
    private AutenticacaoService autenticacaoService;

    private Membro membro;

    @BeforeEach
    void setUp() {
        membro = new Membro(1, "Fulano de Tal", "fulano@example.com", "senhaCriptografada", "12345678900", null, null, null);
    }

    @Test
    void deveEncontrarMembroPorEmail() {
        when(repository.findByEmail("fulano@example.com")).thenReturn(Optional.of(membro));

        UserDetails resultado = autenticacaoService.loadUserByUsername("fulano@example.com");

        assertThat(resultado).isEqualTo(membro);
        verify(repository, never()).findByCpf(eq("fulano@example.com"));
    }

    @Test
    void deveEncontrarMembroPorCpfQuandoNaoEncontradoPorEmail() {
        when(repository.findByEmail("12345678900")).thenReturn(Optional.empty());
        when(repository.findByCpf("12345678900")).thenReturn(Optional.of(membro));

        UserDetails resultado = autenticacaoService.loadUserByUsername("12345678900");

        assertThat(resultado).isEqualTo(membro);
    }

    @Test
    void deveLancarExcecaoQuandoIdentificadorNaoExiste() {
        when(repository.findByEmail("inexistente@example.com")).thenReturn(Optional.empty());
        when(repository.findByCpf("inexistente@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> autenticacaoService.loadUserByUsername("inexistente@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
