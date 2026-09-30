package br.org.larescolaredencao.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.org.larescolaredencao.model.Secao;

public interface SecaoRepository extends JpaRepository<Secao, Long> {

    /** Seções de uma página específica, para não misturar o conteúdo de páginas diferentes. */
    List<Secao> findByPaginaIdOrderByGrupoAscOrdemAsc(Long idPagina);

    Page<Secao> findByPaginaId(Long idPagina, Pageable pageable);

    Page<Secao> findByPaginaIdAndGrupo(Long idPagina, String grupo, Pageable pageable);

    @Query("select max(s.ordem) from Secao s where s.pagina.id = :idPagina and s.grupo = :grupo")
    Integer findMaxOrdemByPaginaIdAndGrupo(@Param("idPagina") Long idPagina, @Param("grupo") String grupo);

    @Query("SELECT s FROM Secao s WHERE s.pagina.id = :idPagina AND " +
           "(:grupo IS NULL OR s.grupo = :grupo) AND (" +
           "   LOWER(s.titulo) LIKE :search ESCAPE '!' OR " +
           "   LOWER(s.grupo) LIKE :search ESCAPE '!' OR " +
           "   LOWER(cast(s.ordem as String)) LIKE :search ESCAPE '!' OR " +
           "   LOWER(s.conteudo) LIKE :search ESCAPE '!' OR " +
           "   LOWER(s.imagem) LIKE :search ESCAPE '!'" +
           ")")
    Page<Secao> buscar(@Param("idPagina") Long idPagina, @Param("grupo") String grupo,
                       @Param("search") String search, Pageable pageable);
}
