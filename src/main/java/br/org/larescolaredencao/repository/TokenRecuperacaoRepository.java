package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.TokenRecuperacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TokenRecuperacaoRepository extends JpaRepository<TokenRecuperacao, Long> {

    Optional<TokenRecuperacao> findByToken(String token);

    @Modifying
    @Query("DELETE FROM TokenRecuperacao t WHERE t.membro = :membro")
    void deleteByMembro(@Param("membro") Membro membro);
}