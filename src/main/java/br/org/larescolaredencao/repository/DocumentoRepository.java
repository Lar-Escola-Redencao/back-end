package br.org.larescolaredencao.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.org.larescolaredencao.model.Documento;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    /** Documentos cujas seções pertencem à página informada. */
    Page<Documento> findBySecaoPaginaId(Long idPagina, Pageable pageable);

    @Query("SELECT d FROM Documento d WHERE d.secao.pagina.id = :idPagina AND (" +
           "   LOWER(d.titulo) LIKE :search ESCAPE '!' OR " +
           "   LOWER(d.arquivo) LIKE :search ESCAPE '!' OR " +
           "   LOWER(d.secao.titulo) LIKE :search ESCAPE '!'" +
           ")")
    Page<Documento> buscar(@Param("idPagina") Long idPagina, @Param("search") String search, Pageable pageable);
}
