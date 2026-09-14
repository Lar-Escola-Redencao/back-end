package br.org.larescolaredencao.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.org.larescolaredencao.model.Turma;

public interface TurmaRepository extends JpaRepository<Turma, Integer> {
    List<Turma> findByUnidadeId(Integer unidadeId);

    List<Turma> findByUnidadeIdAndIdNot(Integer unidadeId, Integer id);
}
