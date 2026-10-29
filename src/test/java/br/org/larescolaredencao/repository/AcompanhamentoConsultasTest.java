package br.org.larescolaredencao.repository;

import jakarta.persistence.Entity;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import java.util.List;

/** Valida os HQLs e os mapeamentos reais sem conectar ou alterar o banco. */
class AcompanhamentoConsultasTest {
    @Test void consultasPaginadasCompilamComOsMapeamentosDaAplicacao() throws Exception {
        var config = new Configuration();
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        config.setProperty("hibernate.boot.allow_jdbc_metadata_access", "false");
        config.setProperty("hibernate.hbm2ddl.auto", "none");
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        for (var entidade : scanner.findCandidateComponents("br.org.larescolaredencao.model")) {
            config.addAnnotatedClass(Class.forName(entidade.getBeanClassName()));
        }
        try (var factory = config.buildSessionFactory(); var session = factory.openSession()) {
            String ocorrencias = OcorrenciaRepository.class.getMethod("listarAcompanhamento", Integer.class,
                    boolean.class, List.class, String.class, Pageable.class).getAnnotation(Query.class).value();
            String entrevistas = EntrevistaSocialRepository.class.getMethod("listarAcompanhamento", Integer.class,
                    String.class, Pageable.class).getAnnotation(Query.class).value();
            session.createQuery(ocorrencias, br.org.larescolaredencao.model.Ocorrencia.class);
            session.createQuery(entrevistas, br.org.larescolaredencao.model.EntrevistaSocial.class);
        }
    }
}
