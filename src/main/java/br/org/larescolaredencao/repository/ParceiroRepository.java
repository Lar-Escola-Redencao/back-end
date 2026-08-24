package br.org.larescolaredencao.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.org.larescolaredencao.model.Parceiro;


public interface ParceiroRepository extends JpaRepository<Parceiro, Integer> {
	
}
