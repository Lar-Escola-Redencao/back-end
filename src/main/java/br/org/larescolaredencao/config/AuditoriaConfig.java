package br.org.larescolaredencao.config;

import br.org.larescolaredencao.audit.HistoricoHibernateListener;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AuditoriaConfig {

    @Bean
    HibernatePropertiesCustomizer propriedadesAuditoria(HistoricoHibernateListener listener) {
        IntegratorProvider integradores = () -> List.of(criarIntegrador(listener));

        return propriedades -> {
            propriedades.put("org.hibernate.envers.audit_table_suffix", "_aud");
            propriedades.put("org.hibernate.envers.revision_field_name", "rev");
            propriedades.put("org.hibernate.envers.revision_type_field_name", "revtype");
            propriedades.put("org.hibernate.envers.store_data_at_delete", true);
            propriedades.put("org.hibernate.envers.revision_on_collection_change", false);
            propriedades.put("hibernate.integrator_provider", integradores);
        };
    }

    private Integrator criarIntegrador(HistoricoHibernateListener listener) {
        return new Integrator() {
            @Override
            public void integrate(
                    Metadata metadados,
                    BootstrapContext contexto,
                    SessionFactoryImplementor sessionFactory) {

                EventListenerRegistry registro = sessionFactory.getServiceRegistry()
                        .getService(EventListenerRegistry.class);

                registro.appendListeners(EventType.POST_INSERT, listener);
                registro.appendListeners(EventType.POST_UPDATE, listener);
                registro.appendListeners(EventType.POST_DELETE, listener);
                registro.appendListeners(EventType.PRE_COLLECTION_UPDATE, listener);
                registro.appendListeners(EventType.PRE_COLLECTION_REMOVE, listener);
                registro.appendListeners(EventType.POST_COLLECTION_UPDATE, listener);
                registro.appendListeners(EventType.POST_COLLECTION_REMOVE, listener);
                registro.appendListeners(EventType.POST_COLLECTION_RECREATE, listener);
            }

            @Override
            public void disintegrate(
                    SessionFactoryImplementor sessionFactory,
                    SessionFactoryServiceRegistry registro) {
            }
        };
    }
}
