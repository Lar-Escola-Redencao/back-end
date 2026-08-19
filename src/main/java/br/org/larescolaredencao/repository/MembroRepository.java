package br.org.larescolaredencao.repository;


import br.org.larescolaredencao.model.Membro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MembroRepository extends JpaRepository<Membro, Integer>{
	Optional<Membro> findByEmail(String email);
}
