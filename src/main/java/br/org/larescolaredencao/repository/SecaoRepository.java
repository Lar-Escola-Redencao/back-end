package br.org.larescolaredencao.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.org.larescolaredencao.model.Secao;

public interface SecaoRepository extends JpaRepository<Secao, Long> {

    /** Seções de uma página específica, para não misturar o conteúdo de páginas diferentes. */
    List<Secao> findByPaginaIdOrderByIdAsc(Long idPagina);

    Page<Secao> findByPaginaId(Long idPagina, Pageable pageable);
}
