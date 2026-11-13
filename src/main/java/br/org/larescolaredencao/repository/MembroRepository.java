package br.org.larescolaredencao.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.dto.ResponsavelHistoricoDTO;

public interface MembroRepository extends JpaRepository<Membro, Integer> {

    boolean existsByUnidadesIdAndPapelNomePapelNot(Integer unidadeId, String nomePapel);

    @Query("SELECT new br.org.larescolaredencao.dto.ResponsavelHistoricoDTO(m.id, m.nomeCompleto) FROM Membro m")
    List<ResponsavelHistoricoDTO> listarResponsaveisHistorico();
    
    @EntityGraph(attributePaths = {"unidades", "papel"})
    Optional<Membro> findByEmail(String email);
    
    Optional<Membro> findByCpf(String cpf);
    Page<Membro> findByPapelId(Integer idPapel, Pageable pageable);

    default Optional<Membro> buscarPorCpf(String cpf) {
        String digitos = Membro.somenteDigitos(cpf);
        String comMascara = digitos.replaceFirst("^(\\d{3})(\\d{3})(\\d{3})(\\d{2})$", "$1.$2.$3-$4");
        return findByCpf(digitos).or(() -> findByCpf(comMascara));
    }

    // A consulta usa EXISTS para cada membro ocupar uma única posição antes da paginação.
    String FILTRO_BUSCA = "(:search IS NULL OR " +
           "   LOWER(m.nomeCompleto) LIKE :search ESCAPE '!' OR " +
           "   LOWER(m.email) LIKE :search ESCAPE '!' OR " +
           "   LOWER(m.cpf) LIKE :search ESCAPE '!' OR " +
           "   LOWER(m.endereco) LIKE :search ESCAPE '!' OR " +
           "   LOWER(m.telefone) LIKE :search ESCAPE '!' OR " +
           "   LOWER(m.papel.nomePapel) LIKE :search ESCAPE '!' OR " +
           "   (:searchDigitos IS NOT NULL AND REPLACE(REPLACE(m.cpf, '.', ''), '-', '') LIKE :searchDigitos) OR " +
           "   (:searchDigitos IS NOT NULL AND REPLACE(REPLACE(REPLACE(REPLACE(m.telefone, '(', ''), ')', ''), '-', ''), ' ', '') LIKE :searchDigitos)" +
           ")";

    String FILTROS_ADMINISTRADOR =
           "(:idUnidade IS NULL OR m.papel.nomePapel = 'ADMINISTRADOR' OR " +
           " EXISTS (SELECT 1 FROM Membro mu JOIN mu.unidades u WHERE mu.id = m.id AND u.id = :idUnidade)) AND " +
           "(:idPapel IS NULL OR m.papel.id = :idPapel)";

    @Query("SELECT m FROM Membro m WHERE " + FILTROS_ADMINISTRADOR + " AND " + FILTRO_BUSCA)
    Page<Membro> buscar(@Param("search") String search, @Param("searchDigitos") String searchDigitos,
                        @Param("idPapel") Integer idPapel, @Param("idUnidade") Integer idUnidade, Pageable pageable);

    @Query("SELECT m FROM Membro m WHERE " + FILTROS_ADMINISTRADOR)
    Page<Membro> filtrar(@Param("idPapel") Integer idPapel, @Param("idUnidade") Integer idUnidade, Pageable pageable);

    String ORDEM_PERFIL = " ORDER BY CASE WHEN m.id = :idProprio THEN 0 ELSE 1 END";

    @Query(value = "SELECT m FROM Membro m WHERE " + FILTROS_ADMINISTRADOR + " AND " + FILTRO_BUSCA + ORDEM_PERFIL,
            countQuery = "SELECT COUNT(m) FROM Membro m WHERE " + FILTROS_ADMINISTRADOR + " AND " + FILTRO_BUSCA)
    Page<Membro> listarParaAdministrador(@Param("idProprio") Integer idProprio,
            @Param("idPapel") Integer idPapel, @Param("idUnidade") Integer idUnidade,
            @Param("search") String search, @Param("searchDigitos") String searchDigitos, Pageable pageable);

    String FILTROS_COORDENADOR =
           "(m.id = :idProprio OR m.papel.nomePapel = 'MONITOR') AND " +
           "EXISTS (SELECT 1 FROM Membro mu JOIN mu.unidades u WHERE mu.id = m.id AND u.id IN :idsUnidades " +
           "        AND (:idUnidade IS NULL OR u.id = :idUnidade)) AND " +
           "(:idPapel IS NULL OR m.papel.id = :idPapel) AND " + FILTRO_BUSCA;

    @Query(value = "SELECT m FROM Membro m WHERE " + FILTROS_COORDENADOR + ORDEM_PERFIL,
            countQuery = "SELECT COUNT(m) FROM Membro m WHERE " + FILTROS_COORDENADOR)
    Page<Membro> listarParaCoordenador(@Param("idProprio") Integer idProprio, @Param("idsUnidades") List<Integer> idsUnidades,
            @Param("idPapel") Integer idPapel, @Param("idUnidade") Integer idUnidade,
            @Param("search") String search, @Param("searchDigitos") String searchDigitos, Pageable pageable);
}
