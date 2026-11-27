package br.org.larescolaredencao.config;

import br.org.larescolaredencao.service.PaginaService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Reutiliza a coluna existente; não exige alteração de schema nem execução manual de SQL. */
@Component
public class InicializarOrdemTransparencia implements ApplicationRunner {
    private final PaginaService paginas;

    public InicializarOrdemTransparencia(PaginaService paginas) {
        this.paginas = paginas;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        paginas.listarSecoesParaOrdenacao();
    }
}
