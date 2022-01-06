package io.smallrye.context.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.persistence.EntityManager;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.ws.rs.container.CompletionCallback;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
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

import com.arjuna.ats.jta.logging.jtaLogger;

import io.smallrye.context.test.util.AbstractTest;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class FullStackTest extends AbstractTest {
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
        weld.initialize();

        JTAUtils.startJTATM();

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
                Map<String, Object> contextMap = new HashMap<String, Object>();
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
                System.err.println("BEGIN transaction " + transaction);

                boolean success = false;
                try {
                    Thread.currentThread().setContextClassLoader(new CPClassLoader());
                    super.invoke(req, response);
                    success = true;
                } finally {
                    // tear down request contexts
                    if (req.getAsyncContext().isSuspended()) {
                        // make sure we remove the CDI context
                        cdiContext.deactivate();
                        cdiContext.dissociate(contextMap);
                        Transaction t2;
                        try {
                            t2 = transactionManager.suspend();
                            System.err.println("SUSPEND " + t2);
                        } catch (SystemException e) {
                            throw new RuntimeException(e);
                        }
                        // clear it later
                        req.getAsyncContext().getAsyncResponse().register((CompletionCallback) (t) -> {
                            try {
                                System.err.println("RESUME " + t2);
                                Transaction currentTransaction = transactionManager.getTransaction();
                                if (currentTransaction != t2) {
                                    if (currentTransaction != null)
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

            private void terminateContext(BoundRequestContext cdiContext, Map<String, Object> contextMap,
                    EntityManager entityManager,
                    TransactionManager tm, Transaction tx, boolean success) {
                System.err.println("END: " + success + " " + tx);
                try {
                    endTransaction(tm, tx, success);
                } catch (Exception e) {
                    // let's not throw here
                    e.printStackTrace();
                }
                // only need to terminate CDI context for "/test/async", otherwise context propagation handles it
                if (cdiContext.isActive()) {
                    cdiContext.invalidate();
                    cdiContext.deactivate();
                    cdiContext.dissociate(contextMap);
                }
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
        deployment.start();

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
        JTAUtils.stop();
    }

    @Test
    public void fullStack() throws IOException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(new HttpGet("http://localhost:8080/test"))) {
                assertEquals(200, response.getStatusLine().getStatusCode());
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                assertEquals("OK", body);
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet("http://localhost:8080/test/async"))) {
                assertEquals(500, response.getStatusLine().getStatusCode());
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet("http://localhost:8080/test/async-working"))) {
                assertEquals(200, response.getStatusLine().getStatusCode());
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                assertEquals("OK", body);
            }
        }

        // TODO RestAssured depends on on Jakarta EE 8 classes (JAX-B specifically)
        //  see https://github.com/rest-assured/rest-assured/issues/1510
        //RestAssured.when().get("/test").then().statusCode(200).body(is("OK"));
        //RestAssured.when().get("/test/async").then().statusCode(500);
        //RestAssured.when().get("/test/async-working").then().statusCode(200).body(is("OK"));
    }
}
