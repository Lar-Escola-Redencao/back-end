package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.Assistido;
import br.org.larescolaredencao.model.ContatoAssistido;
import br.org.larescolaredencao.model.ContatoAssistidoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContatoAssistidoRepository extends JpaRepository<ContatoAssistido, ContatoAssistidoId> {
    List<ContatoAssistido> findByAssistido(Assistido assistido);
}