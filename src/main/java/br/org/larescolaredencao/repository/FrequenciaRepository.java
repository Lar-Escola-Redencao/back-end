package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.Frequencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FrequenciaRepository extends JpaRepository<Frequencia, Integer> {
    Optional<Frequencia> findByMatriculaIdAndDataRegistro(Integer matriculaId, LocalDate dataRegistro);
    List<Frequencia> findByMatriculaTurmaIdAndDataRegistro(Integer turmaId, LocalDate dataRegistro);
    void deleteByMatriculaIdAndDataRegistroGreaterThanEqual(Integer matriculaId, LocalDate dataRegistro);
    void deleteByMatriculaIdAndDataRegistroAfter(Integer matriculaId, LocalDate dataRegistro);

    @Query("SELECT f FROM Frequencia f JOIN FETCH f.matricula m JOIN FETCH m.usuario " +
           "WHERE m.turma.id = :turmaId AND f.dataRegistro BETWEEN :inicio AND :fim")
    List<Frequencia> findPorTurmaNoPeriodo(@Param("turmaId") Integer turmaId,
                                           @Param("inicio") LocalDate inicio,
                                           @Param("fim") LocalDate fim);
}
