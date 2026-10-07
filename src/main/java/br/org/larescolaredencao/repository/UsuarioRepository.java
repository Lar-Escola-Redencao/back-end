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

    String FILTRO_BUSCA_USUARIO =
           "NOT EXISTS (SELECT 1 FROM Matricula mx WHERE mx.usuario = u AND mx.status = 'EXCLUIDO') AND (" +
           "   LOWER(u.nomeCompleto) LIKE :search ESCAPE '!' OR " +
           "   LOWER(u.cpf) LIKE :search ESCAPE '!' OR " +
           "   LOWER(format(u.dataNascimento as 'dd/MM/yyyy')) LIKE :search ESCAPE '!' OR " +
           "   LOWER(u.documentoAuxiliar) LIKE :search ESCAPE '!' OR " +
           "   LOWER(cast(u.tipoDocumento as String)) LIKE :search ESCAPE '!' OR " +
           "   LOWER(u.cadUnico) LIKE :search ESCAPE '!' OR " +
           "   LOWER(u.endereco) LIKE :search ESCAPE '!' OR " +
           "   LOWER(u.bairro) LIKE :search ESCAPE '!' OR " +
           "   LOWER(u.escola) LIKE :search ESCAPE '!' OR " +
           "   LOWER(u.raEscolar) LIKE :search ESCAPE '!' OR " +
           "   (:searchDigitos IS NOT NULL AND REPLACE(REPLACE(u.cpf, '.', ''), '-', '') LIKE :searchDigitos) OR " +
           "   (:searchDigitos IS NOT NULL AND REPLACE(u.cep, '-', '') LIKE :searchDigitos)" +
           ")";

    // Regra do fantasma: usuário com matrícula EXCLUIDO (soft delete) nunca é retornado.
    @Query("SELECT DISTINCT u FROM Usuario u " +
           "JOIN Matricula m ON m.usuario = u " +
           "WHERE m.status = 'ATIVO' AND " + FILTRO_BUSCA_USUARIO)
    List<Usuario> buscarAtivos(@Param("search") String search, @Param("searchDigitos") String searchDigitos);

    @Query("SELECT DISTINCT u FROM Usuario u " +
           "JOIN Matricula m ON m.usuario = u " +
           "JOIN m.turma t " +
           "JOIN t.unidade un " +
           "WHERE m.status = 'ATIVO' AND un IN " +
           "(SELECT um FROM Membro mem JOIN mem.unidades um WHERE mem.id = :membroId) AND " + FILTRO_BUSCA_USUARIO)
    List<Usuario> buscarAtivosByMembroId(@Param("membroId") Integer membroId, @Param("search") String search,
                                         @Param("searchDigitos") String searchDigitos);

    @Modifying
    @Query("UPDATE Usuario u SET u.imagemPerfil = null WHERE u.id = :id")
    void removerImagemPerfil(@Param("id") Integer id);
}