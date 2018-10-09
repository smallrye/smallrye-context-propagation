package io.smallrye.concurrency.inject;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessInjectionPoint;

import org.eclipse.microprofile.concurrent.ThreadContext;

public class ConcurrencyExtension implements Extension {
	
    public void collectConfigProducer(@Observes ProcessInjectionPoint<?, ThreadContext> pip) {
    	System.err.println("Injection point: "+pip.getInjectionPoint());
    }
    
    public void registerInjectionPoints(@Observes AfterBeanDiscovery abd, BeanManager bm) {
    	System.err.println("Registering injection bean");
    	abd.addBean(new ThreadContextInjectionBean(bm));
    }

}
