package br.org.larescolaredencao.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.org.larescolaredencao.model.Pagina;

public interface PaginaRepository extends JpaRepository<Pagina, Long> {

    Optional<Pagina> findByNome(String nome);
}
