package br.org.larescolaredencao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import br.org.larescolaredencao.model.Membro;
import java.util.Optional;

public interface MembroRepository extends JpaRepository<Membro, Integer> {
	Optional<Membro> findByEmail(String email);
    Optional<Membro> findByCpf(String cpf);
}