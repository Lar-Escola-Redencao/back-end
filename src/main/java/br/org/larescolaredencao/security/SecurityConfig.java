package br.org.larescolaredencao.security;

import br.org.larescolaredencao.config.RecursosPublicos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
                    req.requestMatchers("/error").permitAll();
                    req.requestMatchers(HttpMethod.GET, "/pix").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/pix/codigo").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/pix/codigo/*/agradecimento").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/auth/login").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/auth/esqueci-minha-senha").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/auth/validar-codigo").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/auth/redefinir-senha").permitAll();            
                    
                    // Documentos de saúde: servidos por ArquivoSaudeController, que valida a unidade do usuário.
                    req.requestMatchers(HttpMethod.GET, "/uploads/usuarios/saude/*", "/uploads/saude/*")
                            .hasAnyRole("ADMINISTRADOR", "COORDENADOR", "MONITOR");
                    // Conteúdo institucional publicado no site (ver RecursosPublicos).
                    req.requestMatchers(HttpMethod.GET, RecursosPublicos.padroesUpload()).permitAll();
                    // Demais arquivos (inclusive as URLs antigas das fotos em /uploads/usuarios/) nunca são
                    // servidos diretamente: a foto é consultada por GET /usuarios/{id}/foto.
                    req.requestMatchers("/uploads/**").denyAll();

                    req.requestMatchers(HttpMethod.GET,
                            "/evento/**",
                            "/parceiro/**",
                            "/diretoria/**",
                            "/unidade/**").permitAll();

                    req.requestMatchers(HttpMethod.GET,
                            "/rede-social/todas",
                            "/rede-social/{id:\\d+}").permitAll();

                    req.requestMatchers(HttpMethod.GET,
                            "/paginas/sobre/indicadores",
                            "/paginas/{id:\\d+}",
                            "/paginas/{id:\\d+}/secoes",
                            "/paginas/secoes/{id:\\d+}",
                            "/paginas/documentos/{id:\\d+}/download").permitAll();

                    req.requestMatchers("/historico-alteracoes/**").hasRole("ADMINISTRADOR");
                    req.requestMatchers("/acompanhamento/usuarios/*/entrevistas", "/acompanhamento/usuarios/*/entrevistas/**")
                            .hasAnyRole("ADMINISTRADOR", "COORDENADOR");
                    req.requestMatchers("/acompanhamento/**").hasAnyRole("ADMINISTRADOR", "COORDENADOR", "MONITOR");
                    req.requestMatchers("/paginas/**").hasRole("ADMINISTRADOR");

                    req.requestMatchers(HttpMethod.GET, "/turmas/**")
                            .hasAnyRole("ADMINISTRADOR", "COORDENADOR", "MONITOR");
                    req.requestMatchers("/unidade/**", "/turmas/**", "/evento/**", "/parceiro/**",
                            "/diretoria/**", "/rede-social/**").hasRole("ADMINISTRADOR");

                    req.requestMatchers(HttpMethod.GET, "/membro/me").authenticated();
                    req.requestMatchers(HttpMethod.PUT, "/membro/me").authenticated();  
                    req.requestMatchers(HttpMethod.PUT, "/membro/me/senha").authenticated();
                    req.requestMatchers(HttpMethod.POST, "/membro/me/senha/validar").authenticated();
                    
                    req.requestMatchers("/membro/**", "/papel/**").hasAnyRole("ADMINISTRADOR", "COORDENADOR");
                    req.requestMatchers(HttpMethod.GET, "/usuarios", "/usuarios/{id:[0-9]+}",
                            "/usuarios/{id:[0-9]+}/arquivos-saude",
                            "/usuarios/{id:[0-9]+}/foto").hasAnyRole("ADMINISTRADOR", "COORDENADOR", "MONITOR");
                    req.requestMatchers("/usuarios/**").hasAnyRole("ADMINISTRADOR", "COORDENADOR");
                    req.requestMatchers("/contatos/buscar").hasAnyRole("ADMINISTRADOR", "COORDENADOR");
                    req.requestMatchers("/contatos/**").hasAnyRole("ADMINISTRADOR", "COORDENADOR");
                    
                    req.requestMatchers("/frequencia/**", "/ocorrencia/**")
                            .hasAnyRole("ADMINISTRADOR", "COORDENADOR", "MONITOR");
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

    /**
     * O SecurityFilter roda somente dentro da cadeia do Spring Security. Sem isto o Boot também o
     * registraria como filtro de servlet e, conforme a ordem, ele seria executado fora da cadeia e
     * ignorado dentro dela (OncePerRequestFilter), deixando a requisição sem autenticação.
     */
    @Bean
    public FilterRegistrationBean<SecurityFilter> securityFilterRegistration(SecurityFilter filter) {
        FilterRegistrationBean<SecurityFilter> registro = new FilterRegistrationBean<>(filter);
        registro.setEnabled(false);
        return registro;
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
        configuration.setExposedHeaders(List.of("Content-Disposition"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
