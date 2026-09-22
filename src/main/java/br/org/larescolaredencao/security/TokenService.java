package br.org.larescolaredencao.security;

import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.enums.Perfil;
import br.org.larescolaredencao.repository.PapelRepository;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    @Autowired
    private PapelRepository papelRepository;

    public String gerarToken(Membro membro, boolean lembrarMe) {
        var perfil = recuperarPerfil(membro);
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("lar-redencao-api")
                    .withSubject(membro.getEmail())
                    .withClaim("role", perfil.name())
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

    private Perfil recuperarPerfil(Membro membro) {
        var papel = papelRepository.findByMembroId(membro.getId())
                .orElseThrow(() -> new IllegalStateException("Membro sem papel associado: " + membro.getEmail()));
        return Perfil.fromNomePapel(papel.getNomePapel());
    }

    private Instant gerarDataExpiracao(boolean lembrarMe) {
        return lembrarMe
                ? Instant.now().plus(3, ChronoUnit.DAYS)
                : Instant.now().plus(4, ChronoUnit.HOURS);
    }
}
