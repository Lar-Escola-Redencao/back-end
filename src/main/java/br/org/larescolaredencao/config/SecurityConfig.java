package br.org.larescolaredencao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Desabilita a proteção CSRF, necessária para testarmos APIs REST no Postman
            .csrf(AbstractHttpConfigurer::disable)
            // Configura a autorização das requisições
            .authorizeHttpRequests(auth -> auth
                // Libera temporariamente o acesso a todas as rotas
                .anyRequest().permitAll() 
            );

        return http.build();
    }
}