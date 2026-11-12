package br.org.larescolaredencao.validacao;

import java.util.List;

import br.org.larescolaredencao.dto.AtualizarContatoDTO;
import br.org.larescolaredencao.dto.AtualizarMembroDTO;
import br.org.larescolaredencao.dto.AtualizarPerfilDTO;
import br.org.larescolaredencao.dto.AtualizarUsuarioDTO;
import br.org.larescolaredencao.dto.CadastroUsuarioCompletoDTO;
import br.org.larescolaredencao.dto.ContatoDTO;
import br.org.larescolaredencao.dto.CriarMembroDTO;
import br.org.larescolaredencao.dto.LoginRequestDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CpfValidacaoTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();
    private static final List<Class<?>> FORMULARIOS = List.of(
            CriarMembroDTO.class, AtualizarMembroDTO.class, AtualizarPerfilDTO.class,
            CadastroUsuarioCompletoDTO.class, AtualizarUsuarioDTO.class,
            ContatoDTO.class, AtualizarContatoDTO.class);

    @AfterAll
    static void fechar() {
        FACTORY.close();
    }

    private Object formulario(Class<?> tipo, String cpf) throws ReflectiveOperationException {
        Object formulario = tipo.getConstructor().newInstance();
        tipo.getMethod("setCpf", String.class).invoke(formulario, cpf);
        return formulario;
    }

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "529.982.247-25", "11144477735", "12345678909"})
    void aceitaCpfValidoEmTodosOsCadastrosEEdicoes(String cpf) throws ReflectiveOperationException {
        for (Class<?> tipo : FORMULARIOS) {
            assertThat(VALIDATOR.validateProperty(formulario(tipo, cpf), "cpf"))
                    .as(tipo.getSimpleName()).isEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000000", "11111111111", "99999999999", "52998224724", "52998224715",
            "5299822472", "529982247255", "abc52998224725", "529-982-247-25"})
    void rejeitaCpfInvalidoEmTodosOsCadastrosEEdicoes(String cpf) throws ReflectiveOperationException {
        for (Class<?> tipo : FORMULARIOS) {
            assertThat(VALIDATOR.validateProperty(formulario(tipo, cpf), "cpf"))
                    .as(tipo.getSimpleName())
                    .anyMatch(erro -> erro.getMessage().equals("Informe um CPF válido."));
        }
    }

    @Test
    void contatosPermitemCpfOpcionalInclusiveVazio() throws ReflectiveOperationException {
        for (Class<?> tipo : List.of(ContatoDTO.class, AtualizarContatoDTO.class)) {
            assertThat(VALIDATOR.validateProperty(formulario(tipo, null), "cpf")).isEmpty();
            assertThat(VALIDATOR.validateProperty(formulario(tipo, ""), "cpf")).isEmpty();
        }
    }

    @Test
    void validaOsContatosAninhadosNoCadastroDoUsuario() {
        ContatoDTO contato = new ContatoDTO();
        contato.setCpf("00000000000");
        CadastroUsuarioCompletoDTO usuario = new CadastroUsuarioCompletoDTO();
        usuario.setContatos(List.of(contato));
        assertThat(VALIDATOR.validate(usuario))
                .anyMatch(erro -> erro.getPropertyPath().toString().equals("contatos[0].cpf")
                        && erro.getMessage().equals("Informe um CPF válido."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "529.982.247-25", "admin@exemplo.com"})
    void loginAceitaCpfValidoOuEmail(String identificador) {
        assertThat(VALIDATOR.validate(new LoginRequestDTO(identificador, "senha", false))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"000.000.000-00", "52998224724", "5299822472", "abc52998224725"})
    void loginRejeitaCpfInvalido(String identificador) {
        assertThat(VALIDATOR.validate(new LoginRequestDTO(identificador, "senha", false)))
                .anyMatch(erro -> erro.getPropertyPath().toString().equals("identificador"));
    }
}
