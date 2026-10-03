package br.org.larescolaredencao.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.org.larescolaredencao.model.Usuario;
import br.org.larescolaredencao.model.enums.TipoDocumento;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByCpf(String cpf);

    Optional<Usuario> findFirstByDocumentoAuxiliarAndTipoDocumento(String documentoAuxiliar, TipoDocumento tipoDocumento);

    @Query("SELECT DISTINCT u FROM Usuario u " +
           "JOIN Matricula m ON m.usuario = u " +
           "JOIN m.turma t " +
           "JOIN t.unidade un " +
           "WHERE m.status = 'ATIVO' AND un IN " +
           "(SELECT um FROM Membro mem JOIN mem.unidades um WHERE mem.id = :membroId)")
    List<Usuario> findAtivosByMembroId(@Param("membroId") Integer membroId);

    @Query("SELECT DISTINCT u FROM Usuario u " +
           "JOIN Matricula m ON m.usuario = u " +
           "WHERE m.status = 'ATIVO'")
    List<Usuario> findAtivos();
    
    @Query("SELECT DISTINCT u FROM Usuario u " +
            "JOIN Matricula m ON m.usuario = u " +
            "JOIN m.turma t " +
            "WHERE m.status = 'ATIVO' AND t.unidade.id = :unidadeId AND LOWER(u.nomeCompleto) LIKE LOWER(CONCAT('%', :termo, '%'))")
     List<Usuario> searchAtivosByTermoAndUnidade(@Param("termo") String termo, @Param("unidadeId") Integer unidadeId);
    
    @Modifying
    @Query("UPDATE Usuario u SET u.imagemPerfil = null WHERE u.id = :id")
    void removerImagemPerfil(@Param("id") Integer id);
}