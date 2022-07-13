package io.smallrye.context.test;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

@ApplicationScoped
public class EntityManagerProvider {

    private EntityManagerFactory emf;

    @PostConstruct
    public void setupEntityManagerFactory() {
        emf = Persistence.createEntityManagerFactory("persistence");
    }

    @Produces
    @RequestScoped
    public EntityManager start() {
        return emf.createEntityManager();
    }

    public void close(@Disposes EntityManager em) {
        throw  new RuntimeException("Testing if dispose gets called! No worries");
    }
}
