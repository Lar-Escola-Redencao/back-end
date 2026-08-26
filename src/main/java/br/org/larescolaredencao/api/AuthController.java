package br.org.larescolaredencao.api;

import br.org.larescolaredencao.dto.LoginRequestDTO;
import br.org.larescolaredencao.dto.LoginResponseDTO;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager manager;

    @Autowired
    private TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> efetuarLogin(@RequestBody LoginRequestDTO dados) {
        var authenticationToken = new UsernamePasswordAuthenticationToken(dados.email(), dados.senha());
        
        var authentication = manager.authenticate(authenticationToken);

        var lembrarMe = Boolean.TRUE.equals(dados.lembrarMe());
        var tokenJWT = tokenService.gerarToken((Membro) authentication.getPrincipal(), lembrarMe);

        return ResponseEntity.ok(new LoginResponseDTO(tokenJWT));
    }
}
