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
           "(SELECT COUNT(ca) FROM ContatoAssistido ca WHERE ca.contato = c) = 0 " +
           "OR c IN (" +
           "   SELECT ca2.contato FROM ContatoAssistido ca2 " +
           "   JOIN ca2.assistido a " +
           "   JOIN Matricula m ON m.assistido = a " +
           "   JOIN m.turma t " +
           "   JOIN t.unidade u " +
           "   WHERE m.status = 'ATIVO' AND u IN (" +
           "       SELECT un FROM Membro mem JOIN mem.unidades un WHERE mem.id = :membroId" +
           "   )" +
           ")")
    Page<Contato> findVisibleByMembroId(@Param("membroId") Integer membroId, Pageable pageable);

    @Query("SELECT DISTINCT c FROM Contato c WHERE " +
           "(LOWER(c.nomeCompleto) LIKE LOWER(CONCAT('%', :termo, '%')) OR c.telefone LIKE CONCAT('%', :termo, '%')) AND " +
           "((SELECT COUNT(ca) FROM ContatoAssistido ca WHERE ca.contato = c) = 0 " +
           "OR c IN (" +
           "   SELECT ca2.contato FROM ContatoAssistido ca2 " +
           "   JOIN ca2.assistido a " +
           "   JOIN Matricula m ON m.assistido = a " +
           "   JOIN m.turma t " +
           "   JOIN t.unidade u " +
           "   WHERE m.status = 'ATIVO' AND u IN (" +
           "       SELECT un FROM Membro mem JOIN mem.unidades un WHERE mem.id = :membroId" +
           "   )" +
           "))")
    List<Contato> searchVisibleByMembroIdAndTermo(@Param("membroId") Integer membroId, @Param("termo") String termo);
}