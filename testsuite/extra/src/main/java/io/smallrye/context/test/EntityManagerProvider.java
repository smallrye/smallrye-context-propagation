package io.smallrye.context.test;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

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
        System.err.println("Creating EM");
        return emf.createEntityManager();
    }

    public void close(@Disposes EntityManager em) {
        System.err.println("Disposing EM");
        em.close();
    }
}
