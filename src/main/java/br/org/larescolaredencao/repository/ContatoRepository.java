package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.Contato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContatoRepository extends JpaRepository<Contato, Integer> {
    Optional<Contato> findByTelefoneAndNomeCompletoAndEmail(String telefone, String nomeCompleto, String email);
}