package br.org.larescolaredencao.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.org.larescolaredencao.model.Unidade;

public interface UnidadeRepository extends JpaRepository<Unidade, Integer> {
    Optional<Unidade> findByNome(String nome);
}
