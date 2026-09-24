package br.org.larescolaredencao.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}