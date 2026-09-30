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

    @Query("SELECT m FROM Matricula m WHERE m.turma.id = :turmaId " +
           "AND m.dataIngresso <= :dataFimDia " +
           "AND (m.dataDesligamento IS NULL OR m.dataDesligamento >= :data)")
    List<Matricula> findHistoricoAtivasPorTurmaEData(@Param("turmaId") Integer turmaId, 
                                                     @Param("dataFimDia") LocalDateTime dataFimDia, 
                                                     @Param("data") LocalDate data);
}