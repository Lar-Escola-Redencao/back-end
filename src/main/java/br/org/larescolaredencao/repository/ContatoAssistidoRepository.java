package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.Assistido;
import br.org.larescolaredencao.model.ContatoAssistido;
import br.org.larescolaredencao.model.ContatoAssistidoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContatoAssistidoRepository extends JpaRepository<ContatoAssistido, ContatoAssistidoId> {
    List<ContatoAssistido> findByAssistido(Assistido assistido);
    
    List<ContatoAssistido> findByContatoId(Integer contatoId);
    
    long countByContatoId(Integer contatoId);
    
    long countByAssistido(Assistido assistido);
    
    Optional<ContatoAssistido> findByAssistidoAndPrincipalTrue(Assistido assistido);
    
    Optional<ContatoAssistido> findByAssistidoIdAndContatoId(Integer assistidoId, Integer contatoId);
}