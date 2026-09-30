package br.org.larescolaredencao.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.org.larescolaredencao.model.Membro;

public interface MembroRepository extends JpaRepository<Membro, Integer> {
    Optional<Membro> findByEmail(String email);
    Optional<Membro> findByCpf(String cpf);
    Page<Membro> findByPapelId(Integer idPapel, Pageable pageable);

    default Optional<Membro> buscarPorCpf(String cpf) {
        String digitos = Membro.somenteDigitos(cpf);
        String comMascara = digitos.replaceFirst("^(\\d{3})(\\d{3})(\\d{3})(\\d{2})$", "$1.$2.$3-$4");
        return findByCpf(digitos).or(() -> findByCpf(comMascara));
    }

    // Membro não tem soft delete (a exclusão é física). A senha fica de fora da busca.
    @Query("SELECT m FROM Membro m WHERE " +
           "(:idPapel IS NULL OR m.papel.id = :idPapel) AND (" +
           "   LOWER(m.nomeCompleto) LIKE :search ESCAPE '!' OR " +
           "   LOWER(m.email) LIKE :search ESCAPE '!' OR " +
           "   LOWER(m.cpf) LIKE :search ESCAPE '!' OR " +
           "   LOWER(m.endereco) LIKE :search ESCAPE '!' OR " +
           "   LOWER(m.telefone) LIKE :search ESCAPE '!' OR " +
           "   LOWER(m.papel.nomePapel) LIKE :search ESCAPE '!' OR " +
           "   (:searchDigitos IS NOT NULL AND REPLACE(REPLACE(m.cpf, '.', ''), '-', '') LIKE :searchDigitos) OR " +
           "   (:searchDigitos IS NOT NULL AND REPLACE(REPLACE(REPLACE(REPLACE(m.telefone, '(', ''), ')', ''), '-', ''), ' ', '') LIKE :searchDigitos)" +
           ")")
    Page<Membro> buscar(@Param("search") String search, @Param("searchDigitos") String searchDigitos,
                        @Param("idPapel") Integer idPapel, Pageable pageable);
}
