package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.Frequencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FrequenciaRepository extends JpaRepository<Frequencia, Integer> {
    Optional<Frequencia> findByMatriculaIdAndDataRegistro(Integer matriculaId, LocalDate dataRegistro);
    List<Frequencia> findByMatriculaTurmaIdAndDataRegistro(Integer turmaId, LocalDate dataRegistro);
}