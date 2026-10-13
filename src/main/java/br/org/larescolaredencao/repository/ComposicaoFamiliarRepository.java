package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.ComposicaoFamiliar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComposicaoFamiliarRepository extends JpaRepository<ComposicaoFamiliar, Integer> {

    List<ComposicaoFamiliar> findByIdUsuarioOrderByIdAsc(Integer idUsuario);

    void deleteByIdUsuario(Integer idUsuario);
}