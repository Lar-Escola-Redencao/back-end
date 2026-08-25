package br.org.larescolaredencao.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.org.larescolaredencao.model.Parceiro;

public interface ParceiroRepository extends JpaRepository<Parceiro, Long> {
	List<Parceiro> findByAtivoTrue();
}
