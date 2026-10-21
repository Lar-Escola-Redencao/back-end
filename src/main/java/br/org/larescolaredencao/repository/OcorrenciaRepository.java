package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.Ocorrencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @Query("select o from Ocorrencia o join fetch o.matricula m join fetch m.usuario join fetch o.membro " +
           "where m.turma.id = :turmaId and o.dataOcorrencia >= :inicio and o.dataOcorrencia < :fimExclusivo")
    List<Ocorrencia> findPorTurmaNoPeriodo(
            @Param("turmaId") Integer turmaId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fimExclusivo") LocalDateTime fimExclusivo);
    @Modifying
    @Query("delete from Ocorrencia o where o.matricula.id = :matriculaId and cast(o.dataOcorrencia as LocalDate) >= :dataOcorrencia")
    void deleteByMatriculaIdAndDataOcorrenciaGreaterThanEqual(
            @Param("matriculaId") Integer matriculaId,
            @Param("dataOcorrencia") LocalDate dataOcorrencia);
    @Modifying
    @Query("delete from Ocorrencia o where o.matricula.id = :matriculaId and cast(o.dataOcorrencia as LocalDate) > :dataOcorrencia")
    void deleteByMatriculaIdAndDataOcorrenciaAfter(
            @Param("matriculaId") Integer matriculaId,
            @Param("dataOcorrencia") LocalDate dataOcorrencia);
}
