package br.org.larescolaredencao.security;

import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Papel;
import br.org.larescolaredencao.repository.PapelRepository;
import com.auth0.jwt.JWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private PapelRepository papelRepository;

    @InjectMocks
    private TokenService tokenService;

    private Membro membro;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenService, "secret", "segredo-de-teste");
        membro = new Membro(1, "Fulano de Tal", "fulano@example.com", "senha", "12345678900", null, null, null);
    }

    @ParameterizedTest
    @CsvSource({
            "1, ADMINISTRADOR",
            "2, COORDENADOR",
            "3, MONITOR"
    })
    void deveInjetarRoleDoPapelNoToken(Integer idPapel, String nomePapel) {
        when(papelRepository.findByMembroId(1)).thenReturn(Optional.of(new Papel(idPapel, nomePapel, null)));

        String token = tokenService.gerarToken(membro, false);

        assertThat(JWT.decode(token).getClaim("role").asString()).isEqualTo(nomePapel);
        assertThat(tokenService.getSubject(token)).isEqualTo("fulano@example.com");
    }

    @Test
    void deveFalharQuandoMembroNaoPossuiPapel() {
        when(papelRepository.findByMembroId(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.gerarToken(membro, false))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveFalharQuandoPapelNaoPertenceAHierarquia() {
        when(papelRepository.findByMembroId(1)).thenReturn(Optional.of(new Papel(9, "VISITANTE", null)));

        assertThatThrownBy(() -> tokenService.gerarToken(membro, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
