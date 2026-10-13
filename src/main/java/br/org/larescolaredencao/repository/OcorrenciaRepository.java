package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.Ocorrencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Integer> {
    List<Ocorrencia> findByMatriculaTurmaIdAndDataOcorrencia(Integer turmaId, LocalDate dataOcorrencia);
    List<Ocorrencia> findByMatriculaIdAndDataOcorrencia(Integer matriculaId, LocalDate dataOcorrencia);
    void deleteByMatriculaIdAndDataOcorrenciaGreaterThanEqual(Integer matriculaId, LocalDate dataOcorrencia);
    void deleteByMatriculaIdAndDataOcorrenciaAfter(Integer matriculaId, LocalDate dataOcorrencia);
}
