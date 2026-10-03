package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.Usuario;
import br.org.larescolaredencao.model.ContatoUsuario;
import br.org.larescolaredencao.model.ContatoUsuarioId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContatoUsuarioRepository extends JpaRepository<ContatoUsuario, ContatoUsuarioId> {
    List<ContatoUsuario> findByUsuario(Usuario usuario);

    List<ContatoUsuario> findByContatoId(Integer contatoId);

    long countByContatoId(Integer contatoId);

    long countByUsuario(Usuario usuario);

    Optional<ContatoUsuario> findByUsuarioAndPrincipalTrue(Usuario usuario);

    Optional<ContatoUsuario> findByUsuarioIdAndContatoId(Integer usuarioId, Integer contatoId);

    void deleteByUsuarioId(Integer usuarioId);
}