package br.org.larescolaredencao.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.Usuario;
import br.org.larescolaredencao.model.enums.StatusMatricula;
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

    String FILTROS_LISTAGEM =
           "NOT EXISTS (SELECT 1 FROM Matricula mx WHERE mx.usuario = u AND mx.status = 'EXCLUIDO') " +
           "AND m.status <> 'EXCLUIDO' " +
           "AND (" +
           "   m.status = 'ATIVO' OR " +
           "   (NOT EXISTS (SELECT 1 FROM Matricula ma WHERE ma.usuario = u AND ma.status = 'ATIVO') " +
           "    AND m.dataIngresso = (SELECT MAX(mr.dataIngresso) FROM Matricula mr WHERE mr.usuario = u AND mr.status <> 'EXCLUIDO'))" +
           ") " +
           
           "AND (:membroId IS NULL OR un IN (SELECT um FROM Membro mem JOIN mem.unidades um WHERE mem.id = :membroId)) " +
           "AND (:idUnidade IS NULL OR un.id = :idUnidade) " +
           "AND (:idTurma IS NULL OR t.id = :idTurma) " +
           "AND (:statusMatricula IS NULL OR m.status = :statusMatricula) " +
           "AND (:search IS NULL OR " +
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

    @Query("SELECT DISTINCT u FROM Usuario u " +
           "JOIN Matricula m ON m.usuario = u JOIN m.turma t JOIN t.unidade un WHERE " +
           FILTROS_LISTAGEM + " ORDER BY u.id DESC")
    List<Usuario> buscarListagem(@Param("membroId") Integer membroId,
                                 @Param("search") String search,
                                 @Param("searchDigitos") String searchDigitos,
                                 @Param("idUnidade") Integer idUnidade,
                                 @Param("idTurma") Integer idTurma,
                                 @Param("statusMatricula") StatusMatricula statusMatricula);

    // Uma matrícula por usuário, usando a mais recente entre as que atendem aos filtros.
    String MATRICULA_LISTAGEM =
           " FROM Matricula m JOIN m.usuario u JOIN m.turma t JOIN t.unidade un WHERE " + FILTROS_LISTAGEM +
           " AND m.id = (SELECT mr.id FROM Matricula mr WHERE mr.usuario = u AND mr.status <> 'EXCLUIDO' " +
           " AND (:membroId IS NULL OR mr.turma.unidade IN " +
           "      (SELECT um FROM Membro mem JOIN mem.unidades um WHERE mem.id = :membroId)) " +
           " AND (:idUnidade IS NULL OR mr.turma.unidade.id = :idUnidade) " +
           " AND (:idTurma IS NULL OR mr.turma.id = :idTurma) " +
           " AND (:statusMatricula IS NULL OR mr.status = :statusMatricula) " +
           " ORDER BY CASE WHEN mr.status = 'ATIVO' THEN 0 ELSE 1 END, mr.dataIngresso DESC, mr.id DESC LIMIT 1)";

    @Query(value = "SELECT m" + MATRICULA_LISTAGEM, countQuery = "SELECT COUNT(m)" + MATRICULA_LISTAGEM)
    Page<Matricula> buscarListagemPaginada(@Param("membroId") Integer membroId,
            @Param("search") String search, @Param("searchDigitos") String searchDigitos,
            @Param("idUnidade") Integer idUnidade, @Param("idTurma") Integer idTurma,
            @Param("statusMatricula") StatusMatricula statusMatricula, Pageable pageable);

    default void removerImagemPerfil(Integer id) {
        findById(id).ifPresent(usuario -> {
            usuario.setImagemPerfil(null);
            save(usuario);
        });
    }
}
