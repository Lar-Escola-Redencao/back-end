package br.org.larescolaredencao.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.org.larescolaredencao.model.Evento;
import br.org.larescolaredencao.model.enums.TipoEvento;

public interface EventoRepository extends JpaRepository<Evento, Integer> {
	boolean existsByTituloAndDataEvento(String titulo, LocalDateTime dataEvento);
	Page<Evento> findByTipoEvento(TipoEvento tipoEvento, Pageable pageable);
}