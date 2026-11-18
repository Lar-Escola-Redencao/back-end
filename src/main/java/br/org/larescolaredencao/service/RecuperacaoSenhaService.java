package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.RecuperacaoSenhaRespostaDTO;
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
    private final ProtecaoAutenticacaoService protecao;

    public RecuperacaoSenhaService(MembroRepository membroRepository,
                                   TokenRecuperacaoRepository tokenRecuperacaoRepository,
                                   EmailService emailService,
                                   PasswordEncoder passwordEncoder,
                                   ProtecaoAutenticacaoService protecao) {
        this.membroRepository = membroRepository;
        this.tokenRecuperacaoRepository = tokenRecuperacaoRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = new SecureRandom();
        this.protecao = protecao;
    }

    @Transactional
    public RecuperacaoSenhaRespostaDTO solicitarRecuperacao(String email, String captchaToken, String ip) {
        email = email.trim().toLowerCase(java.util.Locale.ROOT);
        var tentativa = protecao.iniciarRecuperacao(email, captchaToken, ip);
        try {
            enviarCodigo(email);
            return protecao.respostaRecuperacao(tentativa);
        } catch (RuntimeException erro) {
            throw new ProtecaoAutenticacaoException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível solicitar a recuperação. Aguarde e tente novamente.", true, protecao.esperaRecuperacao(tentativa));
        } finally {
            protecao.concluir(tentativa, false);
        }
    }

    private void enviarCodigo(String email) {
        Optional<Membro> membroOpt = membroRepository.findByEmail(email);

        if (membroOpt.isEmpty() || !membroOpt.get().isAtivo()) {
            return;
        }

        Membro membro = membroOpt.get();

        Optional<TokenRecuperacao> tokenExistente = tokenRecuperacaoRepository.findByMembro(membro);
        if (tokenExistente.isPresent()) {
            tokenRecuperacaoRepository.delete(tokenExistente.get());
        }

        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder codigoBuilder = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            codigoBuilder.append(caracteres.charAt(secureRandom.nextInt(caracteres.length())));
        }
        String codigoGerado = codigoBuilder.toString();

        TokenRecuperacao tokenRecuperacao = new TokenRecuperacao();
        tokenRecuperacao.setToken(codigoGerado);
        tokenRecuperacao.setMembro(membro);
        tokenRecuperacao.setDataExpiracao(LocalDateTime.now().plusMinutes(15));

        tokenRecuperacaoRepository.save(tokenRecuperacao);

        emailService.enviarEmailRecuperacao(membro.getEmail(), codigoGerado);
    }

    @Transactional(readOnly = true)
    public void validarCodigo(String token) {
        TokenRecuperacao tokenRecuperacao = tokenRecuperacaoRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido ou não encontrado."));

        if (tokenRecuperacao.getDataExpiracao().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O código fornecido expirou.");
        }
        if (!tokenRecuperacao.getMembro().isAtivo()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido ou não encontrado.");
        }
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
        if (!membro.isAtivo()) {
            tokenRecuperacaoRepository.delete(tokenRecuperacao);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido ou não encontrado.");
        }
        membro.setSenha(passwordEncoder.encode(novaSenha));
        membroRepository.save(membro);

        tokenRecuperacaoRepository.delete(tokenRecuperacao);
    }
}
