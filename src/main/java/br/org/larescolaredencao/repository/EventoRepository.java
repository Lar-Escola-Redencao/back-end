package br.org.larescolaredencao.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.org.larescolaredencao.model.Evento;
import br.org.larescolaredencao.model.enums.TipoEvento;

public interface EventoRepository extends JpaRepository<Evento, Integer> {
	boolean existsByTituloAndDataEvento(String titulo, LocalDateTime dataEvento);
	Page<Evento> findByTipoEvento(TipoEvento tipoEvento, Pageable pageable);

	// INTENCIONAL: não filtra por parceiro.ativo. A página do evento é registro histórico,
	// então quem apoiou aquela edição aparece mesmo que a parceria já tenha sido encerrada.
	@Query("SELECT e FROM Evento e LEFT JOIN FETCH e.parceiros WHERE e.id = :id")
	Optional<Evento> findByIdComParceiros(@Param("id") Integer id);
}