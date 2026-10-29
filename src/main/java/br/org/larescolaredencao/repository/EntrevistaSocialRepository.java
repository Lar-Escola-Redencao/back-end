package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.EntrevistaSocial;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntrevistaSocialRepository extends JpaRepository<EntrevistaSocial, Integer> {
    @org.springframework.data.jpa.repository.Query("""
            select e from EntrevistaSocial e left join Membro autor on autor.id = e.idMembro
            where e.idUsuario = :usuario and (:search is null
                or lower(e.relato) like :search escape '!'
                or lower(autor.nomeCompleto) like :search escape '!'
                or cast(e.dataEntrevista as string) like :search escape '!')
            """)
    org.springframework.data.domain.Page<EntrevistaSocial> listarAcompanhamento(
            @org.springframework.data.repository.query.Param("usuario") Integer usuario,
            @org.springframework.data.repository.query.Param("search") String search,
            org.springframework.data.domain.Pageable pageable);

    void deleteByIdUsuario(Integer idUsuario);
}
