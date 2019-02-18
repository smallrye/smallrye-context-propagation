package io.smallrye.concurrency.test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.concurrent.ThreadContext;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

@Path("test")
public class FullStackResource {
    @Inject
    MyBean myBean;

    @Inject
    EntityManager entityManager;

    @Inject
    ThreadContext threadContext;
    
    @Path("text")
    @GET
    public String text() {
        return "Hello World";
    }
    
    @GET
    public String blockingTest(@Context UriInfo uriInfo) {
        if(myBean == null)
            throw new WebApplicationException("myBean is null");
        // Mark our CDI request context bean
        myBean.setId(42);

        // CDI checks
        MyBean myBean2 = CDI.current().select(MyBean.class).get();
        if(myBean2 == null)
            throw new WebApplicationException("myBean lookup is null");
        if(myBean2.getId() != 42)
            throw new WebApplicationException("myBean lookup is not our own");

        // RESTEasy checks
        if(uriInfo == null)
            throw new WebApplicationException("uriInfo is null");
        uriInfo.getAbsolutePath();

        // JPA checks
        if(entityManager == null)
            throw new WebApplicationException("entityManager is null");
        Long count = (Long) entityManager.createQuery("SELECT COUNT(*) FROM MyEntity").getResultList().get(0);
        if(count != 0)
            throw new WebApplicationException("entityManager count for MyEntity is not 0");
        
        MyEntity entity = new MyEntity();
        entity.name = "stef";
        entityManager.persist(entity);
        
        count = (Long) entityManager.createQuery("SELECT COUNT(*) FROM MyEntity").getResultList().get(0);
        if(count != 1)
            throw new WebApplicationException("entityManager count for MyEntity is not 1");

        entityManager.remove(entity);
        
        return "OK";
    }

    @Path("async")
    @GET
    public CompletionStage<String> testAsync(@Context Vertx vertx, @Context UriInfo uriInfo) {
        CompletableFuture<String> ret = new CompletableFuture<String>();
        // Mark our CDI request context bean
        myBean.setId(42);
        
        WebClient client = WebClient.create(vertx);
        client.get(8080, "localhost", "/test/text").as(BodyCodec.string()).send(res -> {
            if(res.failed())
                ret.completeExceptionally(res.cause());
            else
                ret.complete(res.result().body());
        });
        
        CompletableFuture<String> ret2 = ret.thenApply(body -> {
            // CDI
            if(myBean == null)
                throw new WebApplicationException("myBean is null");
            if(myBean.getId() != 42)
                throw new WebApplicationException("myBean is not our own");
            
            MyBean myBean2 = CDI.current().select(MyBean.class).get();
            if(myBean2 == null)
                throw new WebApplicationException("myBean lookup is null");
            if(myBean2.getId() != 42)
                throw new WebApplicationException("myBean lookup is not our own");
            
            // RESTEasy
            if(uriInfo == null)
                throw new WebApplicationException("uriInfo is null");
            uriInfo.getAbsolutePath();
            return "OK";
        });
        
        return ret2;
    }

    @Path("async-working")
    @GET
    public CompletionStage<String> testAsyncWorking(@Context Vertx vertx, @Context UriInfo uriInfo) {
        CompletableFuture<String> ret = new CompletableFuture<String>();
        // Mark our CDI request context bean
        myBean.setId(42);
        
        WebClient client = WebClient.create(vertx);
        client.get(8080, "localhost", "/test/text").as(BodyCodec.string()).send(res -> {
            if(res.failed())
                ret.completeExceptionally(res.cause());
            else
                ret.complete(res.result().body());
        });
        CompletableFuture<String> ret2 = threadContext.withContextCapture(ret);
        CompletableFuture<String> ret3 = ret2.thenApply(body -> {
            // CDI
            if(myBean == null)
                throw new WebApplicationException("myBean is null");
            if(myBean.getId() != 42)
                throw new WebApplicationException("myBean is not our own");
            
            MyBean myBean2 = CDI.current().select(MyBean.class).get();
            if(myBean2 == null)
                throw new WebApplicationException("myBean lookup is null");
            if(myBean2.getId() != 42)
                throw new WebApplicationException("myBean lookup is not our own");
            
            // RESTEasy
            if(uriInfo == null)
                throw new WebApplicationException("uriInfo is null");
            uriInfo.getAbsolutePath();
            return "OK";
        });
        
        return ret3;
    }
}
