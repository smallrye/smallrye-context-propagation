package io.smallrye.context.test;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import javax.transaction.InvalidTransactionException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.smallrye.context.inject.TransactionServicesImpl;
import io.smallrye.context.test.jta.TransactionalService;
import io.smallrye.context.test.util.AbstractTest;

public class JTATest extends AbstractTest {
    private static Weld weld;

    @BeforeClass
    public static void beforeClass() throws Exception {
        JTAUtils.startJTATM(); // initialise a transaction manager

        weld = new Weld(); // CDI implementation
        weld.addServices(new TransactionServicesImpl());
    }

    @AfterClass
    public static void afterClass() {
        JTAUtils.stop();
    }

    @After
    public void afterTest() {
        try (WeldContainer container = weld.initialize()) {
            // lookup the transaction manager and the test service
            TransactionManager transactionManager = container.select(TransactionManager.class).get();

            try {
                // verify that there is no active transaction
                Transaction transaction = transactionManager.getTransaction();

                Assert.assertNull("transaction still associated", transaction);
            } catch (SystemException ignore) {
                // expected
            }
        }
    }

    @Test
    public void testBasic() throws Exception {
        try {
            ManagedExecutor executor = ManagedExecutor.builder()
                    .maxAsync(2)
                    .propagated(ThreadContext.TRANSACTION)
                    .cleared(ThreadContext.ALL_REMAINING)
                    .build();

            try (WeldContainer container = weld.initialize()) {
                TransactionalService service = container.select(TransactionalService.class).get();
                Assert.assertEquals(1, service.testAsync(executor));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testTransaction() throws Exception {
        try {
            ManagedExecutor executor = ManagedExecutor.builder()
                    .maxAsync(2)
                    .propagated(ThreadContext.TRANSACTION)
                    .cleared(ThreadContext.ALL_REMAINING)
                    .build();

            try (WeldContainer container = weld.initialize()) {
                // lookup the transaction manager and the test service
                TransactionManager transactionManager = container.select(TransactionManager.class).get();
                TransactionalService service = container.select(TransactionalService.class).get();

                transactionManager.begin();
                Assert.assertEquals(0, service.getValue());

                CompletableFuture<Void> stage = executor.runAsync(service::required);
                stage.join();

                try {
                    transactionManager.rollback();
                    if (transactionManager.getTransaction() != null)
                        Assert.fail("transaction still active");
                } catch (SystemException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testTransactionPropagation() throws Exception {
        // create an executor that propagates the transaction context
        ManagedExecutor executor = ManagedExecutor.builder()
                .maxAsync(2)
                .propagated(ThreadContext.TRANSACTION)
                .cleared(ThreadContext.ALL_REMAINING)
                .build();

        try (WeldContainer container = weld.initialize()) {
            // lookup the transaction manager and the test service
            TransactionManager transactionManager = container.select(TransactionManager.class).get();
            TransactionalService service = container.select(TransactionalService.class).get();
            Transaction transaction;

            transactionManager.begin();
            transaction = transactionManager.getTransaction();

            // the service which managages a transaction scoped bean should now be available
            Assert.assertEquals(0, service.getValue());

            try {
                // run various transactional updates on the executor
                CompletableFuture<Void> stage0 = executor.runAsync(() -> {
                    service.required(); // invoke a method that requires a transaction
                    // the service call should have updated the bean in this transaction scope
                    Assert.assertEquals(1, service.getValue());
                }).thenRunAsync(() -> {
                    service.requiresNew();
                    // the service call should have updated a different bean in a different transaction scope
                    Assert.assertEquals(1, service.getValue());
                }).thenRunAsync(() -> {
                    // the service call should have updated the bean in this transaction scope
                    service.supports();
                    Assert.assertEquals(2, service.getValue());
                }).thenRunAsync(() -> {
                    // updating a transaction scoped bean outside of a transacction should fail
                    callServiceExpectFailure((tm, svc) -> svc.never(), transactionManager, service);
                    Assert.assertEquals(2, service.getValue());
                }).thenRunAsync(() -> {
                    // updating a transaction scoped bean outside of a transacction should fail
                    callServiceExpectFailure((tm, svc) -> svc.notSupported(), transactionManager, service);
                    Assert.assertEquals(2, service.getValue());
                }).thenRunAsync(() -> {
                    service.mandatory();
                    // the service call should have updated the bean in this transaction scope
                    Assert.assertEquals(3, service.getValue());
                });

                stage0.join();
            } finally {
                // Must end the transaction in the same thread it was started from.
                // If it is ended by an executor thread then the transaction context provider
                // will re-associate the terminated transaction with the initiating thread when
                // the executor finishes.
                try {
                    // the bean increments should have happened
                    Assert.assertEquals(3, service.getValue());
                    transactionManager.rollback();

                    try {
                        // since the transactio has finished verify that the transaction scoped bean is no longer bound
                        service.getValue();
                        Assert.fail("TransactionScoped bean should only be available from transaction scope");
                    } catch (Exception ignore) {
                    }
                } finally {
                    Assert.assertNull("transaction still activet", transactionManager.getTransaction());
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface TransactionalServiceCall<TransactionManager, TransactionalService> {
        void apply(TransactionManager tm, TransactionalService ts);
    }

    private void callServiceExpectFailure(TransactionalServiceCall<TransactionManager, TransactionalService> op,
            TransactionManager tm, TransactionalService ts) {
        try {
            final Transaction transaction = tm.suspend();
            try {
                op.apply(tm, ts);
                Assert.fail("TransactionScoped bean should only be available from transaction scope");
            } catch (Exception ignore) {
                // expected
            } finally {
                tm.resume(transaction);
            }
        } catch (SystemException | InvalidTransactionException e) {
            Assert.fail("Unable to suspend or resume transaction");
        }
    }

    @Test
    public void testConcurrentTransactionPropagation() throws Exception {
        ManagedExecutor executor = ManagedExecutor.builder()
                .maxAsync(2)
                .propagated(ThreadContext.TRANSACTION)
                .cleared(ThreadContext.ALL_REMAINING)
                .build();

        try (WeldContainer container = weld.initialize()) {
            // lookup the transaction manager and the test service
            TransactionManager transactionManager = container.select(TransactionManager.class).get();
            TransactionalService service = container.select(TransactionalService.class).get();

            transactionManager.begin();

            try {
                Assert.assertEquals(0, service.getValue());

                // run two concurrent transactional bean updates
                CompletableFuture<Void> stage0 = executor.runAsync(service::required);
                CompletableFuture<Void> stage1 = executor.runAsync(service::required);

                CompletableFuture.allOf(stage0, stage1).join();
            } finally {
                // Must end the transaction in the same thread it was started from.
                // If it is ended by an executor thread then the transaction context provider
                // will re-associate the terminated transaction with the initiating thread when
                // the executor finishes.
                try {
                    // the two bean increments should have happened
                    Assert.assertEquals(2, service.getValue());
                    transactionManager.rollback();

                    try {
                        service.getValue();
                        Assert.fail("TransactionScoped bean should only be available from transaction scope");
                    } catch (Exception ignore) {
                    }
                } finally {
                    Assert.assertNull("transaction still activet", transactionManager.getTransaction());
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testRunUnderTransactionOfExecutingThread() {
        /*
         * ThreadContext threadContext = ThreadContext.builder()
         * .propagated(ThreadContext.TRANSACTION)
         * .cleared(ThreadContext.ALL_REMAINING)
         * .build();
         */

        try (WeldContainer container = weld.initialize()) {
            // lookup the transaction manager and the test service
            TransactionManager transactionManager = container.select(TransactionManager.class).get();
            //            TransactionalService service = container.select(TransactionalService.class).get();
            Callable<Boolean> transactionCallable = () -> {
                return transactionManager.getTransaction() != null;
            };

            try {
                transactionManager.begin();
                transactionManager.getTransaction();
                Assert.assertTrue("The callable did not run in a transaction", transactionCallable.call());
            } catch (Exception e) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }

            try {
                transactionManager.rollback();
                if (transactionManager.getTransaction() != null)
                    Assert.fail("transaction still active");
            } catch (SystemException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testTransactionWithUT() throws Exception {
        try {
            ManagedExecutor executor = ManagedExecutor.builder()
                    .maxAsync(2)
                    .propagated(ThreadContext.TRANSACTION)
                    .cleared(ThreadContext.ALL_REMAINING)
                    .build();

            try (WeldContainer container = weld.initialize()) {
                // lookup the transaction manager and the test service
                UserTransaction ut = container.select(UserTransaction.class).get();
                TransactionalService service = container.select(TransactionalService.class).get();

                ut.begin();
                Assert.assertEquals(0, service.getValue());

                CompletableFuture<Void> stage = executor.runAsync(service::required);
                stage.join();

                try {
                    ut.rollback();
                    Assert.assertEquals("transaction still active",
                            ut.getStatus(), Status.STATUS_NO_TRANSACTION);
                } catch (SystemException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testTransactionWithSuspend() throws Exception {
        ManagedExecutor executor = ManagedExecutor.builder()
                .maxAsync(2)
                .propagated(ThreadContext.TRANSACTION)
                .cleared(ThreadContext.ALL_REMAINING)
                .build();

        try (WeldContainer container = weld.initialize()) {
            // lookup the transaction manager and the test service
            UserTransaction ut = container.select(UserTransaction.class).get();
            TransactionalService service = container.select(TransactionalService.class).get();

            ut.begin();

            try {
                CompletableFuture<Void> stage = executor.runAsync(service::required)
                        .whenComplete((result, failure) -> {
                            try {
                                if (failure == null && ut.getStatus() == Status.STATUS_ACTIVE)
                                    ut.commit();
                                else
                                    ut.rollback();
                            } catch (Exception x) {
                                if (failure == null)
                                    throw new CompletionException(x);
                            }
                        });

                stage.get();
            } finally {
                container.select(TransactionManager.class).get().suspend();
            }
        }
    }
}
