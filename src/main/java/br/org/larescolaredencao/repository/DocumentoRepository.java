package br.org.larescolaredencao.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.org.larescolaredencao.model.Documento;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    /** Documentos cujas seções pertencem à página informada. */
    Page<Documento> findBySecaoPaginaId(Long idPagina, Pageable pageable);
}
