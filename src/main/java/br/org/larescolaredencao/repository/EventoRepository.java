package br.org.larescolaredencao.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.org.larescolaredencao.model.Evento;

public interface EventoRepository extends JpaRepository<Evento, Integer> {
}