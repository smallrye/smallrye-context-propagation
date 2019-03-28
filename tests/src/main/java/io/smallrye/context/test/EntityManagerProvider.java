package io.smallrye.context.test;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import com.arjuna.ats.jta.TransactionManager;

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
    
    @Produces
    @ApplicationScoped
    public javax.transaction.TransactionManager transactionManager() {
        return TransactionManager.transactionManager();
    }

}
