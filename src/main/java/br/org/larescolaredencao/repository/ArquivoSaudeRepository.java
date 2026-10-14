package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.ArquivoSaude;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArquivoSaudeRepository extends JpaRepository<ArquivoSaude, Integer> {
    List<ArquivoSaude> findByIdUsuario(Integer idUsuario);
    void deleteByIdUsuario(Integer idUsuario);
    Optional<ArquivoSaude> findByCaminhoArquivo(String caminhoArquivo);
}
