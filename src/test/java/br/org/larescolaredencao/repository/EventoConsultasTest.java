package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.Evento;
import jakarta.persistence.Entity;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.jpa.repository.Query;

class EventoConsultasTest {
    @Test
    void consultasDeCategoriasFiltrosEOrdenacaoCompilamComOMapeamentoAtual() throws Exception {
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
            for (var metodo : EventoRepository.class.getDeclaredMethods()) {
                var consulta = metodo.getAnnotation(Query.class);
                if (consulta == null) {
                    continue;
                }
                session.createQuery(consulta.value(), Evento.class);
                if (!consulta.countQuery().isBlank()) {
                    session.createQuery(consulta.countQuery(), Long.class);
                }
            }
        }
    }
}
