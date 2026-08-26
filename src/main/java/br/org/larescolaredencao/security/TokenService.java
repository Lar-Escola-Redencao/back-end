package br.org.larescolaredencao.security;

import br.org.larescolaredencao.model.Membro;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String gerarToken(Membro membro, boolean lembrarMe) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("lar-redencao-api")
                    .withSubject(membro.getEmail())
                    .withClaim("role", membro.getAuthorities().iterator().next().getAuthority())
                    .withExpiresAt(gerarDataExpiracao(lembrarMe))
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    public String getSubject(String tokenJWT) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("lar-redencao-api")
                    .build()
                    .verify(tokenJWT)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            throw new RuntimeException("Token JWT inválido ou expirado", exception);
        }
    }

    private Instant gerarDataExpiracao(boolean lembrarMe) {
        return lembrarMe
                ? Instant.now().plus(7, ChronoUnit.DAYS)
                : Instant.now().plus(4, ChronoUnit.HOURS);
    }
}
