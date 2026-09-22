package br.org.larescolaredencao.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.org.larescolaredencao.model.Papel;

public interface PapelRepository extends JpaRepository<Papel, Integer> {

    // JOIN membro -> papel via id_papel
    @Query("SELECT p FROM Membro m JOIN m.papel p WHERE m.id = :idMembro")
    Optional<Papel> findByMembroId(@Param("idMembro") Integer idMembro);
}
