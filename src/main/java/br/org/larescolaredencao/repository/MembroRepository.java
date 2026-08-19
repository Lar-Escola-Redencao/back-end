package br.org.larescolaredencao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import br.org.larescolaredencao.model.Membro;

public interface MembroRepository extends JpaRepository<Membro, Integer> {
}