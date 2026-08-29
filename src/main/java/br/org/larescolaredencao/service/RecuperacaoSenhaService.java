package br.org.larescolaredencao.service;

import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.TokenRecuperacao;
import br.org.larescolaredencao.repository.MembroRepository;
import br.org.larescolaredencao.repository.TokenRecuperacaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class RecuperacaoSenhaService {

    private final MembroRepository membroRepository;
    private final TokenRecuperacaoRepository tokenRecuperacaoRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    public RecuperacaoSenhaService(MembroRepository membroRepository,
                                   TokenRecuperacaoRepository tokenRecuperacaoRepository,
                                   EmailService emailService,
                                   PasswordEncoder passwordEncoder) {
        this.membroRepository = membroRepository;
        this.tokenRecuperacaoRepository = tokenRecuperacaoRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = new SecureRandom();
    }

    @Transactional
    public void solicitarRecuperacao(String email) {
        Optional<Membro> membroOpt = membroRepository.findByEmail(email);

        if (membroOpt.isEmpty()) {
            return;
        }

        Membro membro = membroOpt.get();

        tokenRecuperacaoRepository.deleteByMembro(membro);

        String codigoGerado = String.format("%06d", secureRandom.nextInt(1000000));

        TokenRecuperacao tokenRecuperacao = new TokenRecuperacao();
        tokenRecuperacao.setToken(codigoGerado);
        tokenRecuperacao.setMembro(membro);
        tokenRecuperacao.setDataExpiracao(LocalDateTime.now().plusMinutes(15));

        tokenRecuperacaoRepository.save(tokenRecuperacao);

        emailService.enviarEmailRecuperacao(membro.getEmail(), codigoGerado);
    }

    @Transactional
    public void redefinirSenha(String token, String novaSenha) {
        TokenRecuperacao tokenRecuperacao = tokenRecuperacaoRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido ou não encontrado."));

        if (tokenRecuperacao.getDataExpiracao().isBefore(LocalDateTime.now())) {
            tokenRecuperacaoRepository.delete(tokenRecuperacao);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O código fornecido expirou.");
        }

        Membro membro = tokenRecuperacao.getMembro();
        membro.setSenha(passwordEncoder.encode(novaSenha));
        membroRepository.save(membro);

        tokenRecuperacaoRepository.delete(tokenRecuperacao);
    }
}