package br.org.larescolaredencao.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utilitário de apoio ao ambiente local: confere se a senha conhecida bate com o hash do seed
 * e imprime um hash BCrypt novo para redefinir as senhas dos usuários de teste. Não é uma
 * asserção de regra de negócio; existe só para operar o banco local durante os testes manuais.
 */
class SeedPasswordUtilTest {

    private static final String SEED_HASH =
            "$2a$12$HJD3al2Lt3WH5kbpzdqDBuBa21wex7CI/1vBnilmGZQr7XHcemvKG";

    @Test
    void inspecionarSenhasSeed() {
        var encoder = new BCryptPasswordEncoder();
        for (String candidata : new String[] {"admin", "Admin123", "admin123", "123456", "senha"}) {
            System.out.println("MATCH[" + candidata + "]=" + encoder.matches(candidata, SEED_HASH));
        }
        System.out.println("NOVO_HASH_admin=" + encoder.encode("admin"));
    }
}
