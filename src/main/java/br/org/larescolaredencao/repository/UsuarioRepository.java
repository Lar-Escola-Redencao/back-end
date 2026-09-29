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
}