package br.org.larescolaredencao.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.org.larescolaredencao.model.Membro;

public interface MembroRepository extends JpaRepository<Membro, Integer> {
    Optional<Membro> findByEmail(String email);
    Optional<Membro> findByCpf(String cpf);
    Page<Membro> findByPapelId(Integer idPapel, Pageable pageable);
}