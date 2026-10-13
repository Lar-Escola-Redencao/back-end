package br.org.larescolaredencao.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.org.larescolaredencao.model.Pagina;
import jakarta.persistence.LockModeType;

public interface PaginaRepository extends JpaRepository<Pagina, Long> {

    Optional<Pagina> findByNome(String nome);

    /**
     * Trava a linha da página até o fim da transação, serializando gravações
     * concorrentes de grupos singleton (ex.: dois POSTs de telefone ao mesmo tempo).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pagina p where p.id = :id")
    Optional<Pagina> findByIdComBloqueio(@Param("id") Long id);
}
