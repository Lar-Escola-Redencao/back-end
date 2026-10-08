package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.Usuario;
import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.enums.StatusMatricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface MatriculaRepository extends JpaRepository<Matricula, Integer> {
    
    List<Matricula> findByUsuario(Usuario usuario);
    
    List<Matricula> findByStatusAndDataDesligamentoBefore(StatusMatricula status, LocalDate data);
    
    boolean existsByUsuarioAndStatus(Usuario usuario, StatusMatricula status);

    @Query("SELECT m FROM Matricula m JOIN FETCH m.usuario u WHERE m.id IN (" +
            "  SELECT MAX(m2.id) FROM Matricula m2 WHERE m2.turma.id = :turmaId " +
            "  AND m2.dataIngresso < :dataFimDia " +
            "  AND (m2.dataDesligamento IS NULL OR m2.dataDesligamento >= :data) " +
            "  GROUP BY m2.usuario.id" +
            ") AND m.status != 'EXCLUIDO' ORDER BY u.nomeCompleto")
     List<Matricula> findHistoricoAtivasPorTurmaEData(@Param("turmaId") Integer turmaId, 
                                                      @Param("dataFimDia") LocalDateTime dataFimDia, 
                                                      @Param("data") LocalDate data);

    @Query("SELECT COUNT(DISTINCT m.usuario.id) FROM Matricula m WHERE m.status = :status")
    long contarMeninosPorStatus(@Param("status") StatusMatricula status);

    @Query(value = "SELECT COUNT(*) FROM frequencia WHERE id_matricula IN (:ids)", nativeQuery = true)
    long contarFrequenciasPorMatriculas(@Param("ids") List<Integer> ids);

    @Query(value = "SELECT COUNT(*) FROM ocorrencia WHERE id_matricula IN (:ids)", nativeQuery = true)
    long contarOcorrenciasPorMatriculas(@Param("ids") List<Integer> ids);
}
