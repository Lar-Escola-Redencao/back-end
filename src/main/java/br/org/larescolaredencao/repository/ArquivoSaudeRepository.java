package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.ArquivoSaude;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArquivoSaudeRepository extends JpaRepository<ArquivoSaude, Integer> {
    List<ArquivoSaude> findByIdUsuario(Integer idUsuario);
    void deleteByIdUsuario(Integer idUsuario);
}