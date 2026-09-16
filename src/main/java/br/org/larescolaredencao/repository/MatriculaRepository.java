package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.Assistido;
import br.org.larescolaredencao.model.Matricula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatriculaRepository extends JpaRepository<Matricula, Integer> {
    List<Matricula> findByAssistido(Assistido assistido);
}