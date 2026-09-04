package br.org.larescolaredencao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class PaginacaoConfig {

    private static final int TAMANHO_PAGINA_PADRAO = 10;
    private static final int TAMANHO_PAGINA_MAXIMO = 50;

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer paginacaoCustomizer() {
        return resolver -> {
            resolver.setFallbackPageable(PageRequest.of(0, TAMANHO_PAGINA_PADRAO));
            resolver.setMaxPageSize(TAMANHO_PAGINA_MAXIMO);
        };
    }
}
