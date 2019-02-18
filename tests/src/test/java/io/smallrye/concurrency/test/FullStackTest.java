package io.smallrye.concurrency.test;

import static org.hamcrest.Matchers.is;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.spi.CDI;
import javax.persistence.EntityManager;
import javax.ws.rs.container.CompletionCallback;

import org.jboss.resteasy.cdi.CdiInjectorFactory;
import org.jboss.resteasy.cdi.ResteasyCdiExtension;
import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.plugins.server.vertx.VertxJaxrsServer;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.weld.context.bound.BoundRequestContext;
import org.jboss.weld.environment.se.Weld;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class FullStackTest {
    
    private final class MyVertxJaxrsServer extends VertxJaxrsServer {
        public Vertx getVertx() {
            return vertx;
        }
    }

    private MyVertxJaxrsServer vertxJaxrsServer;
    private Weld weld;

    @Before
    public void before() {
        VertxResteasyDeployment deployment = new VertxResteasyDeployment();

        weld = new Weld();
//        weld.addExtension(new VertxExtension());
        weld.initialize();
        
        ResteasyCdiExtension cdiExtension = CDI.current().select(ResteasyCdiExtension.class).get();
        deployment.setActualResourceClasses(cdiExtension.getResources());
        deployment.setInjectorFactoryClass(CdiInjectorFactory.class.getName());
        deployment.getActualProviderClasses().addAll(cdiExtension.getProviders());
        ResteasyProviderFactory providerFactory = ResteasyProviderFactory.newInstance();
        deployment.setProviderFactory(providerFactory);
        deployment.setDispatcher(new SynchronousDispatcher(providerFactory) {

            @Override
            public void invoke(HttpRequest req, HttpResponse response) {
                ResteasyContext.pushContext(Vertx.class, vertxJaxrsServer.getVertx());

                // set up CDI request context
                BoundRequestContext cdiContext = CDI.current().select(BoundRequestContext.class).get();
                Map<String,Object> contextMap = new HashMap<String,Object>();
                cdiContext.associate(contextMap);
                cdiContext.activate();
                
                EntityManager entityManager = CDI.current().select(EntityManager.class).get();
                System.err.println("BEGIN");
                entityManager.getTransaction().begin();
                boolean success = false;
                try {
                    super.invoke(req, response);
                    success = true;
                } finally {
                    // tear down request contexts
                    if(req.getAsyncContext().isSuspended()) {
                        // make sure we remove the CDI context
                        cdiContext.deactivate();
                        cdiContext.dissociate(contextMap);
                        // clear it later
                        req.getAsyncContext().getAsyncResponse().register((CompletionCallback)(t) -> {
                            terminateContext(cdiContext, contextMap, entityManager, t == null);
                        });
                    } else {
                        // clear it now
                        terminateContext(cdiContext, contextMap, entityManager, success);
                    }       
                }
            }

            private void terminateContext(BoundRequestContext cdiContext, Map<String, Object> contextMap, EntityManager entityManager, boolean success) {
                System.err.println("END: "+success);
                if(success)
                    entityManager.getTransaction().commit();
                else
                    entityManager.getTransaction().rollback();
                cdiContext.invalidate();
                cdiContext.deactivate();
                cdiContext.dissociate(contextMap);
                
            }
        });
        
        vertxJaxrsServer = new MyVertxJaxrsServer();
        vertxJaxrsServer.setVertxOptions(new VertxOptions().setEventLoopPoolSize(1));
        vertxJaxrsServer.setDeployment(deployment);
        vertxJaxrsServer.setPort(8080);
        vertxJaxrsServer.setRootResourcePath("/");
        vertxJaxrsServer.setSecurityDomain(null);
        vertxJaxrsServer.start();
    }
    
    @After
    public void after() {
        weld.shutdown();
        vertxJaxrsServer.stop();
    }
    
    @Test
    public void fullStack() {
        RestAssured.when().get("/test").then().statusCode(200).body(is("OK"));
        RestAssured.when().get("/test/async").then().statusCode(500);
        RestAssured.when().get("/test/async-working").then().statusCode(200).body(is("OK"));
    }
}
