package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.Contato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContatoRepository extends JpaRepository<Contato, Integer> {

    Optional<Contato> findByTelefone(String telefone);

    Optional<Contato> findByTelefoneAndNomeCompletoAndEmail(String telefone, String nomeCompleto, String email);

    @Query("SELECT DISTINCT c FROM Contato c WHERE " +
           "NOT EXISTS (SELECT 1 FROM ContatoUsuario ca WHERE ca.contato = c) " +
           "OR c IN (" +
           "   SELECT ca2.contato FROM ContatoUsuario ca2 " +
           "   JOIN ca2.usuario a " +
           "   JOIN Matricula m ON m.usuario = a " +
           "   JOIN m.turma t " +
           "   JOIN t.unidade u " +
           "   WHERE m.status = 'ATIVO' AND u IN (" +
           "       SELECT un FROM Membro mem JOIN mem.unidades un WHERE mem.id = :membroId" +
           "   )" +
           ")")
    Page<Contato> findVisibleByMembroId(@Param("membroId") Integer membroId, Pageable pageable);

    @Query("SELECT DISTINCT c FROM Contato c WHERE " +
           "LOWER(c.nomeCompleto) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "(:termoTelefone IS NOT NULL AND (c.telefone LIKE CONCAT('%', :termoTelefone, '%') OR c.cpf LIKE CONCAT('%', :termoTelefone, '%')))")
    List<Contato> searchGlobalByTermo(@Param("termo") String termo, @Param("termoTelefone") String termoTelefone);

    // Listagem global (Sprint 4). Regra do fantasma: contato vinculado a usuário excluído
    // (matrícula EXCLUIDO) não aparece; contatos sem vínculo continuam visíveis.
    String FILTRO_LISTAGEM = "(" +
           "   LOWER(c.nomeCompleto) LIKE :search ESCAPE '!' OR " +
           "   LOWER(c.telefone) LIKE :search ESCAPE '!' OR " +
           "   LOWER(c.email) LIKE :search ESCAPE '!' OR " +
           "   LOWER(c.cpf) LIKE :search ESCAPE '!' OR " +
           "   LOWER(c.endereco) LIKE :search ESCAPE '!' OR " +
           "   LOWER(c.localTrabalho) LIKE :search ESCAPE '!' OR " +
           "   (:searchDigitos IS NOT NULL AND REPLACE(REPLACE(c.cpf, '.', ''), '-', '') LIKE :searchDigitos) OR " +
           "   (:searchDigitos IS NOT NULL AND REPLACE(REPLACE(REPLACE(REPLACE(c.telefone, '(', ''), ')', ''), '-', ''), ' ', '') LIKE :searchDigitos)" +
           ") AND NOT EXISTS (" +
           "   SELECT 1 FROM ContatoUsuario cu JOIN Matricula m ON m.usuario = cu.usuario " +
           "   WHERE cu.contato = c AND m.status = 'EXCLUIDO'" +
           ")";

    @Query("SELECT c FROM Contato c WHERE " + FILTRO_LISTAGEM)
    Page<Contato> buscar(@Param("search") String search, @Param("searchDigitos") String searchDigitos, Pageable pageable);

    String ORDEM_VINCULOS = " ORDER BY (SELECT COUNT(cu) FROM ContatoUsuario cu WHERE cu.contato = c)";

    @Query(value = "SELECT c FROM Contato c WHERE (:search IS NULL OR (" + FILTRO_LISTAGEM + "))"
            + ORDEM_VINCULOS + " ASC, c.id DESC",
            countQuery = "SELECT COUNT(c) FROM Contato c WHERE (:search IS NULL OR (" + FILTRO_LISTAGEM + "))")
    Page<Contato> listarPorVinculosAsc(@Param("search") String search, @Param("searchDigitos") String searchDigitos,
            Pageable pageable);

    @Query(value = "SELECT c FROM Contato c WHERE (:search IS NULL OR (" + FILTRO_LISTAGEM + "))"
            + ORDEM_VINCULOS + " DESC, c.id DESC",
            countQuery = "SELECT COUNT(c) FROM Contato c WHERE (:search IS NULL OR (" + FILTRO_LISTAGEM + "))")
    Page<Contato> listarPorVinculosDesc(@Param("search") String search, @Param("searchDigitos") String searchDigitos,
            Pageable pageable);
}
