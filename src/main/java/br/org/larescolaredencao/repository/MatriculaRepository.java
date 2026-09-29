package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.Usuario;
import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.enums.StatusMatricula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MatriculaRepository extends JpaRepository<Matricula, Integer> {
    List<Matricula> findByUsuario(Usuario usuario);
    List<Matricula> findByStatusAndDataDesligamentoBefore(StatusMatricula status, LocalDate data);
    boolean existsByUsuarioAndStatus(Usuario usuario, StatusMatricula status);
}