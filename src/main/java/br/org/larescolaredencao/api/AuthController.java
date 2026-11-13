package br.org.larescolaredencao.api;

import br.org.larescolaredencao.dto.LoginRequestDTO;
import br.org.larescolaredencao.dto.LoginResponseDTO;
import br.org.larescolaredencao.service.LoginService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final LoginService loginService;

    public AuthController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> efetuarLogin(@RequestBody @Valid LoginRequestDTO dados, HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(loginService.entrar(dados, request.getRemoteAddr()));
    }
}
