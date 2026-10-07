package br.org.larescolaredencao.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import br.org.larescolaredencao.dto.IndicadoresSobreDTO;
import br.org.larescolaredencao.dto.MatriculaResumoDTO;
import br.org.larescolaredencao.model.enums.StatusMatricula;
import br.org.larescolaredencao.repository.MatriculaRepository;

@Service
public class SobrePublicoService {
    private static final LocalDate DATA_FUNDACAO = LocalDate.of(1978, 8, 29);
    private final MatriculaRepository matriculaRepository;

    public SobrePublicoService(MatriculaRepository matriculaRepository) {
        this.matriculaRepository = matriculaRepository;
    }

    public IndicadoresSobreDTO obterIndicadores() {
        var matriculasAtivas = new MatriculaResumoDTO(
                matriculaRepository.contarMeninosPorStatus(StatusMatricula.ATIVO));
        return new IndicadoresSobreDTO(
                matriculasAtivas.getTotalMeninos(), DATA_FUNDACAO);
    }
}
