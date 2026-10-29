package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.Ocorrencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Integer> {
    @Query("""
            select o from Ocorrencia o
            where o.matricula.usuario.id = :usuario
              and (:admin = true or o.matricula.turma.unidade.id in :unidades)
              and (:search is null or lower(o.descricao) like :search escape '!'
                or lower(o.membro.nomeCompleto) like :search escape '!'
                or lower(o.matricula.turma.unidade.nome) like :search escape '!'
                or lower(cast(o.tipoOcorrencia as string)) like :search escape '!'
                or cast(o.dataOcorrencia as string) like :search escape '!')
            """)
    org.springframework.data.domain.Page<Ocorrencia> listarAcompanhamento(
            @Param("usuario") Integer usuario, @Param("admin") boolean admin,
            @Param("unidades") List<Integer> unidades, @Param("search") String search,
            org.springframework.data.domain.Pageable pageable);

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
