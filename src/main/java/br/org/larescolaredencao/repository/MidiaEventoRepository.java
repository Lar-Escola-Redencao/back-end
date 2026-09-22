package br.org.larescolaredencao.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.org.larescolaredencao.model.MidiaEvento;

public interface MidiaEventoRepository extends JpaRepository<MidiaEvento, Integer> {
    List<MidiaEvento> findByEventoIdOrderByIdAsc(Integer eventoId);
    long countByEventoId(Integer eventoId);
}
