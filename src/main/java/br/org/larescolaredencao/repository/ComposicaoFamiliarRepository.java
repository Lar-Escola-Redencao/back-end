package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.ComposicaoFamiliar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComposicaoFamiliarRepository extends JpaRepository<ComposicaoFamiliar, Integer> {
    void deleteByIdUsuario(Integer idUsuario);
}