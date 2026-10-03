package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.EntrevistaSocial;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntrevistaSocialRepository extends JpaRepository<EntrevistaSocial, Integer> {
    void deleteByIdUsuario(Integer idUsuario);
}