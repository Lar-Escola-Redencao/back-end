package br.org.larescolaredencao.job;

import br.org.larescolaredencao.model.ArquivoSaude;
import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.Usuario;
import br.org.larescolaredencao.model.enums.StatusMatricula;
import br.org.larescolaredencao.repository.ArquivoSaudeRepository;
import br.org.larescolaredencao.repository.MatriculaRepository;
import br.org.larescolaredencao.repository.UsuarioRepository;
import br.org.larescolaredencao.service.ArquivoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class LgpdCronJob {

    private final MatriculaRepository matriculaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ArquivoSaudeRepository arquivoSaudeRepository;
    private final ArquivoService arquivoService;

    public LgpdCronJob(MatriculaRepository matriculaRepository,
                       UsuarioRepository usuarioRepository,
                       ArquivoSaudeRepository arquivoSaudeRepository,
                       ArquivoService arquivoService) {
        this.matriculaRepository = matriculaRepository;
        this.usuarioRepository = usuarioRepository;
        this.arquivoSaudeRepository = arquivoSaudeRepository;
        this.arquivoService = arquivoService;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void rotinaLimpezaDadosSensiveisEgressos() {
        LocalDate umAnoAtras = LocalDate.now().minusYears(1);
        List<Matricula> egressosAntigos = matriculaRepository.findByStatusAndDataDesligamentoBefore(StatusMatricula.EGRESSO, umAnoAtras);

        for (Matricula matricula : egressosAntigos) {
            Usuario usuario = matricula.getUsuario();

            boolean possuiAtiva = matriculaRepository.existsByUsuarioAndStatus(usuario, StatusMatricula.ATIVO);
            if (possuiAtiva) {
                continue;
            }

            if (usuario.getImagemPerfil() != null) {
                arquivoService.deletarArquivo(usuario.getImagemPerfil());
                usuarioRepository.removerImagemPerfil(usuario.getId());
            }
            
            List<ArquivoSaude> arquivosSaude = arquivoSaudeRepository.findByIdUsuario(usuario.getId());
            for (ArquivoSaude arquivoSaude : arquivosSaude) {
                arquivoService.deletarArquivo(arquivoSaude.getCaminhoArquivo());
            }

            if (!arquivosSaude.isEmpty()) {
                arquivoSaudeRepository.deleteByIdUsuario(usuario.getId());
            }
        }
    }
}