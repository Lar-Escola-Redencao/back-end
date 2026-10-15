package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.Ocorrencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDate;
import java.util.List;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Integer> {
    @Query("select o from Ocorrencia o where o.matricula.turma.id = :turmaId and cast(o.dataOcorrencia as LocalDate) = :dataOcorrencia")
    List<Ocorrencia> findByMatriculaTurmaIdAndDataOcorrencia(Integer turmaId, LocalDate dataOcorrencia);
    @Query("select o from Ocorrencia o where o.matricula.id = :matriculaId and cast(o.dataOcorrencia as LocalDate) = :dataOcorrencia")
    List<Ocorrencia> findByMatriculaIdAndDataOcorrencia(Integer matriculaId, LocalDate dataOcorrencia);
    @Modifying
    @Query("delete from Ocorrencia o where o.matricula.id = :matriculaId and cast(o.dataOcorrencia as LocalDate) >= :dataOcorrencia")
    void deleteByMatriculaIdAndDataOcorrenciaGreaterThanEqual(Integer matriculaId, LocalDate dataOcorrencia);
    @Modifying
    @Query("delete from Ocorrencia o where o.matricula.id = :matriculaId and cast(o.dataOcorrencia as LocalDate) > :dataOcorrencia")
    void deleteByMatriculaIdAndDataOcorrenciaAfter(Integer matriculaId, LocalDate dataOcorrencia);
}
