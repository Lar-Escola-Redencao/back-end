package br.org.larescolaredencao.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    /**
     * Publica apenas os diretórios de conteúdo institucional. Arquivos internos (fotos de usuários,
     * documentos de saúde) não têm handler estático, então conhecer a URL não basta para acessá-los.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        for (String diretorio : RecursosPublicos.DIRETORIOS_UPLOAD) {
            registry.addResourceHandler("/uploads/" + diretorio + "/**")
                    .addResourceLocations(base.resolve(diretorio).toUri().toString());
        }
    }
}
