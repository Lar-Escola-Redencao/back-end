package br.org.larescolaredencao.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.org.larescolaredencao.model.Parceiro;

public interface ParceiroRepository extends JpaRepository<Parceiro, Long> {
	List<Parceiro> findByAtivoTrue();

    @Query("SELECT p FROM Parceiro p WHERE " +
           "   LOWER(p.nome) LIKE :search ESCAPE '!' OR " +
           "   LOWER(p.logo) LIKE :search ESCAPE '!'")
    Page<Parceiro> buscar(@Param("search") String search, Pageable pageable);
}
