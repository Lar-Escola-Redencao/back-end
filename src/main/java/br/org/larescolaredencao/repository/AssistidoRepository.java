package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.Assistido;
import br.org.larescolaredencao.model.enums.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssistidoRepository extends JpaRepository<Assistido, Integer> {
    Optional<Assistido> findByCpf(String cpf);

    Optional<Assistido> findFirstByDocumentoAuxiliarAndTipoDocumento(String documentoAuxiliar, TipoDocumento tipoDocumento);

    @Query("SELECT DISTINCT a FROM Assistido a " +
           "JOIN Matricula m ON m.assistido = a " +
           "JOIN m.turma t " +
           "JOIN t.unidade u " +
           "WHERE m.status = 'ATIVO' AND u IN " +
           "(SELECT un FROM Membro mem JOIN mem.unidades un WHERE mem.id = :membroId)")
    List<Assistido> findAtivosByMembroId(@Param("membroId") Integer membroId);

    @Query("SELECT DISTINCT a FROM Assistido a " +
           "JOIN Matricula m ON m.assistido = a " +
           "JOIN m.turma t " +
           "JOIN t.unidade u " +
           "WHERE m.status = 'ATIVO' AND m.status <> 'EXCLUIDO' AND u IN " +
           "(SELECT un FROM Membro mem JOIN mem.unidades un WHERE mem.id = :membroId) AND (" +
           "   LOWER(a.nomeCompleto) LIKE :search ESCAPE '!' OR " +
           "   LOWER(format(a.dataNascimento as 'dd/MM/yyyy')) LIKE :search ESCAPE '!' OR " +
           "   LOWER(a.cpf) LIKE :search ESCAPE '!' OR " +
           "   LOWER(a.documentoAuxiliar) LIKE :search ESCAPE '!' OR " +
           "   LOWER(cast(a.tipoDocumento as String)) LIKE :search ESCAPE '!' OR " +
           "   LOWER(a.imagemPerfil) LIKE :search ESCAPE '!' OR " +
           "   LOWER(a.endereco) LIKE :search ESCAPE '!' OR " +
           "   (:searchDigitos IS NOT NULL AND REPLACE(REPLACE(a.cpf, '.', ''), '-', '') LIKE :searchDigitos)" +
           ")")
    List<Assistido> buscarAtivosByMembroId(@Param("membroId") Integer membroId, @Param("search") String search,
                                           @Param("searchDigitos") String searchDigitos);
}