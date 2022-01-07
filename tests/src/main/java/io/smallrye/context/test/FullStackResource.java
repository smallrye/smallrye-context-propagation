package io.smallrye.context.test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.context.ThreadContext;

import io.smallrye.context.api.ThreadContextConfig;
import io.smallrye.context.test.jta.TransactionalBean;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

@Path("test")
public class FullStackResource {
    @Inject
    MyBean myBean;

    @Inject
    TransactionalBean myTransactionalBean;

    @Inject
    EntityManager entityManager;

    @Inject
    @ThreadContextConfig(propagated = ThreadContext.ALL_REMAINING, cleared = {})
    ThreadContext threadContext;

    @Path("text")
    @GET
    public String text() {
        testJpaPristine();
        return "Hello World";
    }

    @GET
    public String blockingTest(@Context UriInfo uriInfo) {
        markCdiContext();
        markTransactionalContext();

        testCdiContext();
        testTransactionalContext();
        testResteasyContext(uriInfo);
        testTCCL();

        testJpa1();
        testJpa2();

        return "OK";
    }

    @Path("async")
    @GET
    public CompletionStage<String> testAsync(@Context Vertx vertx, @Context UriInfo uriInfo) {
        markCdiContext();
        markTransactionalContext();
        testJpa1();

        CompletableFuture<String> ret = makeHttpRequest(vertx);
        CompletableFuture<String> ret2 = ret.thenApply(body -> {

            testJpa2();
            testCdiContext();
            testTransactionalContext();
            testResteasyContext(uriInfo);
            testTCCL();

            return "OK";
        });

        CompletableFuture<String> ret3 = ret2.thenApplyAsync(body -> {
            // no JPA test, we already deleted the item
            testCdiContext();
            testTransactionalContext();
            testResteasyContext(uriInfo);
            testTCCL();

            return "OK";
        }, Executors.newSingleThreadExecutor());

        return ret3;
    }

    @Path("async-working")
    @GET
    public CompletionStage<String> testAsyncWorking(@Context Vertx vertx, @Context UriInfo uriInfo) {
        markCdiContext();
        markTransactionalContext();
        testJpa1();

        CompletableFuture<String> ret = makeHttpRequest(vertx);
        CompletableFuture<String> ret2 = threadContext.withContextCapture(ret);
        CompletableFuture<String> ret3 = ret2.thenApply(body -> {

            testJpa2();
            testCdiContext();
            testTransactionalContext();
            testResteasyContext(uriInfo);
            testTCCL();

            return "OK";
        });

        CompletableFuture<String> ret4 = ret3.thenApplyAsync(body -> {
            // no JPA test, we already deleted the item
            testCdiContext();
            testTransactionalContext();
            testResteasyContext(uriInfo);
            testTCCL();

            return "OK";
        }, Executors.newSingleThreadExecutor());

        return ret4;
    }

    // Test kitchen

    private void testTCCL() {
        if (Thread.currentThread().getContextClassLoader() instanceof CPClassLoader == false)
            throw new WebApplicationException("TCCL is not propagated");
    }

    private void testJpa2() {
        Long count = (Long) entityManager.createQuery("SELECT COUNT(*) FROM MyEntity").getResultList().get(0);
        if (count != 1)
            throw new WebApplicationException("entityManager count for MyEntity is not 1");

        if (entityManager.createQuery("DELETE FROM MyEntity").executeUpdate() != 1)
            throw new WebApplicationException("failed to delete MyEntity");
    }

    private void testJpa1() {
        testJpaPristine();

        MyEntity entity = new MyEntity();
        entity.name = "stef";
        entityManager.persist(entity);
    }

    private void testJpaPristine() {
        // JPA checks
        if (entityManager == null)
            throw new WebApplicationException("entityManager is null");
        Long count = (Long) entityManager.createQuery("SELECT COUNT(*) FROM MyEntity").getResultList().get(0);
        if (count != 0)
            throw new WebApplicationException("entityManager count for MyEntity is not 0");
    }

    private void testResteasyContext(UriInfo uriInfo) {
        if (uriInfo == null)
            throw new WebApplicationException("uriInfo is null");
        uriInfo.getAbsolutePath();
    }

    private void markCdiContext() {
        if (myBean == null)
            throw new WebApplicationException("myBean is null");
        // Mark our CDI request context bean
        myBean.setId(42);
    }

    private void testCdiContext() {
        if (myBean.getId() != 42)
            throw new WebApplicationException("myBean is not our own");
        MyBean myBean2 = CDI.current().select(MyBean.class).get();
        if (myBean2 == null)
            throw new WebApplicationException("myBean lookup is null");
        if (myBean2.getId() != 42)
            throw new WebApplicationException("myBean lookup is not our own");
    }

    private void markTransactionalContext() {
        if (myTransactionalBean == null)
            throw new WebApplicationException("myBean is null");
        // Mark our CDI request context bean
        myTransactionalBean.incrementValue();
    }

    private void testTransactionalContext() {
        if (myTransactionalBean.getValue() != 1)
            throw new WebApplicationException("myTransactionalBean is not our own");
        TransactionalBean myTransactionalBean2 = CDI.current().select(TransactionalBean.class).get();
        if (myTransactionalBean2 == null)
            throw new WebApplicationException("myTransactionalBean lookup is null");
        if (myTransactionalBean2.getValue() != 1)
            throw new WebApplicationException("myTransactionalBean lookup is not our own");
    }

    private CompletableFuture<String> makeHttpRequest(Vertx vertx) {
        CompletableFuture<String> ret = new CompletableFuture<String>();
        WebClient client = WebClient.create(vertx);
        client.get(8080, "localhost", "/test/text").as(BodyCodec.string()).send(res -> {
            if (res.failed())
                ret.completeExceptionally(res.cause());
            else
                ret.complete(res.result().body());
        });
        return ret;
    }
}
