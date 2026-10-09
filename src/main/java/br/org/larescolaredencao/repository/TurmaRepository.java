package br.org.larescolaredencao.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.org.larescolaredencao.model.Turma;

public interface TurmaRepository extends JpaRepository<Turma, Integer> {
    List<Turma> findByUnidadeId(Integer unidadeId);

    Page<Turma> findByUnidadeId(Integer unidadeId, Pageable pageable);

    List<Turma> findByUnidadeIdAndIdNot(Integer unidadeId, Integer id);

    @Query("SELECT t FROM Turma t WHERE " +
           "(:unidadeId IS NULL OR t.unidade.id = :unidadeId) AND (" +
           "   LOWER(cast(t.periodo as String)) LIKE :search ESCAPE '!' OR " +
           "   LOWER(format(t.horaInicio as 'HH:mm')) LIKE :search ESCAPE '!' OR " +
           "   LOWER(format(t.horaFim as 'HH:mm')) LIKE :search ESCAPE '!' OR " +
           "   LOWER(t.unidade.nome) LIKE :search ESCAPE '!'" +
           ")")
    Page<Turma> buscar(@Param("search") String search, @Param("unidadeId") Integer unidadeId, Pageable pageable);
}
