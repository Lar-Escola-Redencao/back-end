package br.org.larescolaredencao.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.org.larescolaredencao.model.Secao;
import jakarta.persistence.LockModeType;

public interface SecaoRepository extends JpaRepository<Secao, Long> {

    /** Seções de uma página específica, para não misturar o conteúdo de páginas diferentes. */
    List<Secao> findByPaginaIdOrderByGrupoAscOrdemAsc(Long idPagina);

    Page<Secao> findByPaginaId(Long idPagina, Pageable pageable);

    Page<Secao> findByPaginaIdAndGrupo(Long idPagina, String grupo, Pageable pageable);

    /**
     * Registro de um grupo singleton (ex.: telefone, pix) da página. É leitura com lock
     * (SELECT ... FOR UPDATE) porque, no REPEATABLE READ do InnoDB, uma leitura comum
     * usaria o snapshot do início da transação e não veria o registro criado por uma
     * requisição concorrente que acabou de confirmar.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Secao> findFirstByPaginaIdAndGrupo(Long idPagina, String grupo);

    /**
     * Seção só é encontrada pela rota da página a que pertence. Leitura com lock
     * (SELECT ... FOR UPDATE): PUTs simultâneos na mesma seção são serializados, e cada
     * um enxerga a imagem gravada pelo anterior, removendo-a após o próprio commit.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Secao> findByIdAndPaginaId(Long id, Long idPagina);

    /** Mesmo lock do PUT por página, para o upload avulso de imagem (/paginas/secoes/{id}/imagem). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Secao s where s.id = :id")
    Optional<Secao> findByIdComBloqueio(@Param("id") Long id);

    /**
     * Só o id da página, sem carregar a entidade. A rota antiga de PUT usa isso para não
     * colocar a seção no contexto de persistência antes do lock: o Hibernate devolveria a
     * instância já carregada, com a imagem desatualizada, mesmo após o SELECT ... FOR UPDATE.
     */
    @Query("select s.pagina.id from Secao s where s.id = :id")
    Optional<Long> findIdPaginaById(@Param("id") Long id);

    @Query("select max(s.ordem) from Secao s where s.pagina.id = :idPagina and s.grupo = :grupo")
    Integer findMaxOrdemByPaginaIdAndGrupo(@Param("idPagina") Long idPagina, @Param("grupo") String grupo);
}
