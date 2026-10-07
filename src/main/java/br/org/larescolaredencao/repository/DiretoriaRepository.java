package br.org.larescolaredencao.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.org.larescolaredencao.model.Diretoria;

public interface DiretoriaRepository extends JpaRepository<Diretoria, Long> {

    //Aqui busca no banco as palavras que foram buscadas, fazendo a não diferenciação de maisculo, like para pesquisar qualquer teste entre %%
    @Query("SELECT d FROM Diretoria d WHERE " +
           "   LOWER(d.nome) LIKE :search ESCAPE '!' OR " +
           "   LOWER(d.cargo) LIKE :search ESCAPE '!' OR " +
           "   LOWER(d.foto) LIKE :search ESCAPE '!'")
    Page<Diretoria> buscar(@Param("search") String search, Pageable pageable);
}
