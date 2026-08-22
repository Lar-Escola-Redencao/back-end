package br.org.larescolaredencao.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import br.org.larescolaredencao.model.Evento;

public interface EventoRepository extends JpaRepository<Evento, Integer> {
	boolean existsByTituloAndDataEvento(String titulo, LocalDateTime dataEvento);
}