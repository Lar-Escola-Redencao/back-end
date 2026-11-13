package br.org.larescolaredencao.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

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

    // A senha fica de fora da busca. :ativo nulo traz ativos e inativos.
    @Query("SELECT DISTINCT m FROM Membro m LEFT JOIN m.unidades u WHERE " +
           "(:ativo IS NULL OR m.ativo = :ativo) AND " +
           "(:idUnidade IS NULL OR u.id = :idUnidade OR m.papel.nomePapel = 'ADMINISTRADOR') AND " +
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
    Page<Membro> buscarPorSituacao(@Param("search") String search, @Param("searchDigitos") String searchDigitos,
                        @Param("idPapel") Integer idPapel, @Param("idUnidade") Integer idUnidade,
                        @Param("ativo") Boolean ativo, Pageable pageable);

    default Page<Membro> buscar(String search, String searchDigitos, Integer idPapel, Integer idUnidade,
                                Pageable pageable) {
        return buscarPorSituacao(search, searchDigitos, idPapel, idUnidade, null, pageable);
    }

    @Query("SELECT DISTINCT m FROM Membro m LEFT JOIN m.unidades u WHERE " +
           "(:ativo IS NULL OR m.ativo = :ativo) AND " +
           "(:idPapel IS NULL OR m.papel.id = :idPapel) AND " +
           "(:idUnidade IS NULL OR u.id = :idUnidade OR m.papel.nomePapel = 'ADMINISTRADOR')")
    Page<Membro> filtrarPorSituacao(@Param("idPapel") Integer idPapel, @Param("idUnidade") Integer idUnidade,
                        @Param("ativo") Boolean ativo, Pageable pageable);

    default Page<Membro> filtrar(Integer idPapel, Integer idUnidade, Pageable pageable) {
        return filtrarPorSituacao(idPapel, idUnidade, null, pageable);
    }

    @Query("SELECT DISTINCT m FROM Membro m JOIN m.unidades u WHERE " +
           "m.papel.nomePapel = 'MONITOR' AND u.id IN :idsUnidades AND " +
           "(:ativo IS NULL OR m.ativo = :ativo) AND " +
           "(:idUnidade IS NULL OR u.id = :idUnidade) AND " +
           "(:idPapel IS NULL OR m.papel.id = :idPapel) AND (" +
           "   :search IS NULL OR LOWER(m.nomeCompleto) LIKE :search ESCAPE '!' OR " +
           "   LOWER(m.email) LIKE :search ESCAPE '!' OR " +
           "   LOWER(m.cpf) LIKE :search ESCAPE '!' OR " +
           "   LOWER(m.endereco) LIKE :search ESCAPE '!' OR " +
           "   LOWER(m.telefone) LIKE :search ESCAPE '!' OR " +
           "   LOWER(m.papel.nomePapel) LIKE :search ESCAPE '!' OR " +
           "   (:searchDigitos IS NOT NULL AND REPLACE(REPLACE(m.cpf, '.', ''), '-', '') LIKE :searchDigitos) OR " +
           "   (:searchDigitos IS NOT NULL AND REPLACE(REPLACE(REPLACE(REPLACE(m.telefone, '(', ''), ')', ''), '-', ''), ' ', '') LIKE :searchDigitos)" +
           ")")
    Page<Membro> listarMonitoresDasUnidadesPorSituacao(@Param("idsUnidades") List<Integer> idsUnidades,
            @Param("idPapel") Integer idPapel, @Param("idUnidade") Integer idUnidade,
            @Param("search") String search, @Param("searchDigitos") String searchDigitos,
            @Param("ativo") Boolean ativo, Pageable pageable);

    default Page<Membro> listarMonitoresDasUnidades(List<Integer> idsUnidades, Integer idPapel, Integer idUnidade,
            String search, String searchDigitos, Pageable pageable) {
        return listarMonitoresDasUnidadesPorSituacao(idsUnidades, idPapel, idUnidade, search, searchDigitos,
                null, pageable);
    }

    /**
     * Trava, na ordem do id, o membro alvo e todos os administradores ativos. Serializa desativações e
     * reativações simultâneas e protege o último administrador ativo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Membro m WHERE m.id = :id OR (m.ativo = true AND " +
           "m.papel.id IN (SELECT p.id FROM Papel p WHERE p.nomePapel = 'ADMINISTRADOR')) ORDER BY m.id")
    List<Membro> bloquearParaGestao(@Param("id") Integer id);
}
