package br.org.larescolaredencao.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.org.larescolaredencao.model.Unidade;

public interface UnidadeRepository extends JpaRepository<Unidade, Integer> {
    Optional<Unidade> findByNome(String nome);

    @Query("SELECT u FROM Unidade u WHERE " +
           "   LOWER(u.nome) LIKE :search ESCAPE '!' OR " +
           "   LOWER(u.endereco) LIKE :search ESCAPE '!' OR " +
           "   LOWER(u.telefone) LIKE :search ESCAPE '!' OR " +
           "   LOWER(u.email) LIKE :search ESCAPE '!' OR " +
           "   LOWER(u.diasFuncionamento) LIKE :search ESCAPE '!' OR " +
           "   LOWER(format(u.horarioAbertura as 'HH:mm')) LIKE :search ESCAPE '!' OR " +
           "   LOWER(format(u.horarioFechamento as 'HH:mm')) LIKE :search ESCAPE '!' OR " +
           "   LOWER(cast(u.idadeMin as String)) LIKE :search ESCAPE '!' OR " +
           "   LOWER(cast(u.idadeMax as String)) LIKE :search ESCAPE '!' OR " +
           "   LOWER(u.corHex) LIKE :search ESCAPE '!' OR " +
           "   LOWER(u.imagem) LIKE :search ESCAPE '!' OR " +
           "   (:searchDigitos IS NOT NULL AND REPLACE(REPLACE(REPLACE(REPLACE(u.telefone, '(', ''), ')', ''), '-', ''), ' ', '') LIKE :searchDigitos)")
    Page<Unidade> buscar(@Param("search") String search, @Param("searchDigitos") String searchDigitos, Pageable pageable);
}
