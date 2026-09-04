package br.org.larescolaredencao.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private SecurityFilter securityFilter;

    @Autowired
    private CustomAuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private CustomAccessDeniedHandler accessDeniedHandler;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> {
                	//req.requestMatchers("/error").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/auth/login").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/auth/esqueci-minha-senha").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/auth/validar-codigo").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/auth/redefinir-senha").permitAll();            
                    
                    req.requestMatchers(HttpMethod.GET, "/uploads/**").permitAll();
                    req.requestMatchers(HttpMethod.GET,
                            "/uploads/parceiros/**",
                            "/uploads/diretoria/**",
                            "/uploads/redes-sociais/**",
                            "/uploads/eventos/**",
                            "/uploads/unidades/**", 
                            "/uploads/transparencia/**").permitAll();

                    req.requestMatchers(HttpMethod.GET,
                            "/evento/**",
                            "/parceiro/**",
                            "/diretoria/**",
                            "/unidade/**").permitAll();

                    req.requestMatchers(HttpMethod.GET,
                            "/rede-social/todas",
                            "/rede-social/{id:\\d+}").permitAll();

                    req.requestMatchers(HttpMethod.GET,
                            "/transparencia",
                            "/transparencia/secoes",
                            "/transparencia/secao/{id:\\d+}",
                            "/transparencia/documento/{id:\\d+}/download").permitAll();

                    req.anyRequest().authenticated();
                })
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}