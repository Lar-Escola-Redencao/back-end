package br.org.larescolaredencao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import br.org.larescolaredencao.model.Papel;

public interface PapelRepository extends JpaRepository<Papel, Integer> {
}