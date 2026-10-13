package br.org.larescolaredencao.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import br.org.larescolaredencao.model.EventoRedeSocial;
import br.org.larescolaredencao.model.EventoRedeSocialId;

public interface EventoRedeSocialRepository extends JpaRepository<EventoRedeSocial, EventoRedeSocialId> {

    // Traz a rede social junto para montar a resposta sem uma consulta extra por item.
    @EntityGraph(attributePaths = "redeSocial")
    List<EventoRedeSocial> findByEventoIdOrderByRedeSocialNomeAsc(Integer idEvento);
}
