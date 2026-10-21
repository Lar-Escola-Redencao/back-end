package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.Ocorrencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Integer> {
    @Query("select o from Ocorrencia o where o.matricula.turma.id = :turmaId and cast(o.dataOcorrencia as LocalDate) = :dataOcorrencia")
    List<Ocorrencia> findByMatriculaTurmaIdAndDataOcorrencia(
            @Param("turmaId") Integer turmaId,
            @Param("dataOcorrencia") LocalDate dataOcorrencia);
    @Query("select o from Ocorrencia o where o.matricula.id = :matriculaId and cast(o.dataOcorrencia as LocalDate) = :dataOcorrencia")
    List<Ocorrencia> findByMatriculaIdAndDataOcorrencia(
            @Param("matriculaId") Integer matriculaId,
            @Param("dataOcorrencia") LocalDate dataOcorrencia);
    @Query("select o from Ocorrencia o where o.matricula.id = :matriculaId and cast(o.dataOcorrencia as LocalDate) >= :dataOcorrencia")
    List<Ocorrencia> buscarParaDeleteByMatriculaIdAndDataOcorrenciaGreaterThanEqual(
            @Param("matriculaId") Integer matriculaId,
            @Param("dataOcorrencia") LocalDate dataOcorrencia);

    default void deleteByMatriculaIdAndDataOcorrenciaGreaterThanEqual(Integer matriculaId, LocalDate dataOcorrencia) {
        deleteAll(buscarParaDeleteByMatriculaIdAndDataOcorrenciaGreaterThanEqual(matriculaId, dataOcorrencia));
    }
    @Query("select o from Ocorrencia o where o.matricula.id = :matriculaId and cast(o.dataOcorrencia as LocalDate) > :dataOcorrencia")
    List<Ocorrencia> buscarParaDeleteByMatriculaIdAndDataOcorrenciaAfter(
            @Param("matriculaId") Integer matriculaId,
            @Param("dataOcorrencia") LocalDate dataOcorrencia);

    default void deleteByMatriculaIdAndDataOcorrenciaAfter(Integer matriculaId, LocalDate dataOcorrencia) {
        deleteAll(buscarParaDeleteByMatriculaIdAndDataOcorrenciaAfter(matriculaId, dataOcorrencia));
    }
}
