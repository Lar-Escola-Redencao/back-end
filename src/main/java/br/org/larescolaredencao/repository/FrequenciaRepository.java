package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.Frequencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FrequenciaRepository extends JpaRepository<Frequencia, Integer> {
    interface ResumoMensal {
        int getAno();
        int getMes();
        long getPresencas();
        long getFaltas();
    }

    @Query("""
            select year(f.dataRegistro) as ano, month(f.dataRegistro) as mes,
                   sum(case when f.presente = true then 1 else 0 end) as presencas,
                   sum(case when f.presente = false then 1 else 0 end) as faltas
            from Frequencia f
            where f.matricula.id in :matriculas and f.dataRegistro between :inicio and :fim
            group by year(f.dataRegistro), month(f.dataRegistro)
            order by year(f.dataRegistro), month(f.dataRegistro)
            """)
    List<ResumoMensal> resumirPorMatriculasEPeriodo(@Param("matriculas") List<Integer> matriculas,
            @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    Optional<Frequencia> findByMatriculaIdAndDataRegistro(Integer matriculaId, LocalDate dataRegistro);
    List<Frequencia> findByMatriculaTurmaIdAndDataRegistro(Integer turmaId, LocalDate dataRegistro);
    void deleteByMatriculaIdAndDataRegistroGreaterThanEqual(Integer matriculaId, LocalDate dataRegistro);
    void deleteByMatriculaIdAndDataRegistroAfter(Integer matriculaId, LocalDate dataRegistro);

    @Query("SELECT f FROM Frequencia f JOIN FETCH f.matricula m JOIN FETCH m.usuario " +
           "WHERE m.turma.id = :turmaId AND f.dataRegistro BETWEEN :inicio AND :fim")
    List<Frequencia> findPorTurmaNoPeriodo(@Param("turmaId") Integer turmaId,
                                           @Param("inicio") LocalDate inicio,
                                           @Param("fim") LocalDate fim);

    @Query("SELECT f FROM Frequencia f JOIN FETCH f.matricula m JOIN FETCH m.turma t JOIN FETCH t.unidade " +
           "WHERE m.id IN :matriculas AND f.dataRegistro BETWEEN :inicio AND :fim " +
           "ORDER BY f.dataRegistro, m.id")
    List<Frequencia> findPorMatriculasNoPeriodo(@Param("matriculas") List<Integer> matriculas,
                                                @Param("inicio") LocalDate inicio,
                                                @Param("fim") LocalDate fim);
}
