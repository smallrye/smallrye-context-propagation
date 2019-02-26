package io.smallrye.concurrency.test;

import static org.hamcrest.Matchers.is;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.spi.CDI;
import javax.persistence.EntityManager;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.ws.rs.container.CompletionCallback;

import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.jta.utils.JNDIManager;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
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
import org.jnp.server.NamingBeanImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.arjuna.ats.jta.logging.jtaLogger;

import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class FullStackTest {
    private final class MyVertxJaxrsServer extends VertxJaxrsServer {
        public Vertx getVertx() {
            return vertx;
        }
    }

    private NamingBeanImpl namingBean = new NamingBeanImpl();
    private MyVertxJaxrsServer vertxJaxrsServer;
    private Weld weld;

    @Before
    public void before() {
        VertxResteasyDeployment deployment = new VertxResteasyDeployment();

        weld = new Weld();
//        weld.addExtension(new VertxExtension());
        weld.initialize();

        try {
            JTATest.initJTATM();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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
                TransactionManager transactionManager = CDI.current().select(TransactionManager.class).get();
                Transaction transaction;
                try {
                    transactionManager.begin();
                    transaction = transactionManager.getTransaction();
                } catch (SystemException | NotSupportedException e) {
                    throw new RuntimeException(e);
                }
                System.err.println("BEGIN transaction "+transaction);
                
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
                        Transaction t2;
                        try {
                            t2 = transactionManager.suspend();
                            System.err.println("SUSPEND "+t2);
                        } catch (SystemException e) {
                            throw new RuntimeException(e);
                        }
                        // clear it later
                        req.getAsyncContext().getAsyncResponse().register((CompletionCallback)(t) -> {
                            try {
                                System.err.println("RESUME "+t2);
                                Transaction currentTransaction = transactionManager.getTransaction();
                                if(currentTransaction != t2) {
                                    if(currentTransaction != null)
                                        transactionManager.suspend();
                                    transactionManager.resume(t2);
                                }
                            } catch (InvalidTransactionException | IllegalStateException | SystemException e) {
                                e.printStackTrace();
                            }
                            terminateContext(cdiContext, contextMap, entityManager, transactionManager, t2, t == null);
                        });
                    } else {
                        // clear it now
                        terminateContext(cdiContext, contextMap, entityManager, transactionManager, transaction, success);
                    }       
                }
            }

            private void terminateContext(BoundRequestContext cdiContext, Map<String, Object> contextMap, EntityManager entityManager, 
                                          TransactionManager tm, Transaction tx, boolean success) {
                System.err.println("END: "+success+" "+tx);
                try {
                    endTransaction(tm, tx, success);
                } catch (Exception e) {
                    // let's not throw here
                    e.printStackTrace();
                }
                cdiContext.invalidate();
                cdiContext.deactivate();
                cdiContext.dissociate(contextMap);
            }
            protected void endTransaction(TransactionManager tm, Transaction tx, boolean success) throws Exception {

                if (tx != tm.getTransaction()) {
                    throw new RuntimeException(jtaLogger.i18NLogger.get_wrong_tx_on_thread());
                }

                if (tx.getStatus() == Status.STATUS_MARKED_ROLLBACK || !success) {
                    tm.rollback();
                } else {
                    tm.commit();
                }
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
        namingBean.stop();
    }
    
    @Test
    public void fullStack() {
        RestAssured.when().get("/test").then().statusCode(200).body(is("OK"));
        RestAssured.when().get("/test/async").then().statusCode(500);
    }

    @Test
    @Ignore
    public void fullStackAsync() {
        RestAssured.when().get("/test/async-working").then().statusCode(200).body(is("OK"));
    }
}
