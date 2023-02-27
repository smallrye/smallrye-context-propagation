package io.smallrye.context.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.smallrye.context.test.jta.TransactionalService;
import io.smallrye.context.test.util.AbstractTest;

class JTATest extends AbstractTest {
    private static Weld weld;

    @BeforeAll
    static void beforeClass() {
        JTAUtils.startJTATM(); // initialise a transaction manager

        weld = new Weld(); // CDI implementation
        weld.addServices(new TransactionServicesImpl());
    }

    @AfterAll
    static void afterClass() {
        JTAUtils.stop();
    }

    @AfterEach
    void afterTest() {
        try (WeldContainer container = weld.initialize()) {
            // lookup the transaction manager and the test service
            TransactionManager transactionManager = container.select(TransactionManager.class).get();

            try {
                // verify that there is no active transaction
                Transaction transaction = transactionManager.getTransaction();

                assertNull(transaction, "transaction still associated");
            } catch (SystemException ignore) {
                // expected
            }
        }
    }

    @Test
    void basic() {
        try {
            ManagedExecutor executor = ManagedExecutor.builder()
                    .maxAsync(2)
                    .propagated(ThreadContext.TRANSACTION)
                    .cleared(ThreadContext.ALL_REMAINING)
                    .build();

            try (WeldContainer container = weld.initialize()) {
                TransactionalService service = container.select(TransactionalService.class).get();
                assertEquals(1, service.testAsync(executor));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void transaction() throws Exception {
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
                assertEquals(0, service.getValue());

                CompletableFuture<Void> stage = executor.runAsync(service::required);
                stage.join();

                try {
                    transactionManager.rollback();
                    if (transactionManager.getTransaction() != null)
                        fail("transaction still active");
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
    void transactionPropagation() throws Exception {
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
            assertEquals(0, service.getValue());

            try {
                // run various transactional updates on the executor
                CompletableFuture<Void> stage0 = executor.runAsync(() -> {
                    service.required(); // invoke a method that requires a transaction
                    // the service call should have updated the bean in this transaction scope
                    assertEquals(1, service.getValue());
                }).thenRunAsync(() -> {
                    service.requiresNew();
                    // the service call should have updated a different bean in a different transaction scope
                    assertEquals(1, service.getValue());
                }).thenRunAsync(() -> {
                    // the service call should have updated the bean in this transaction scope
                    service.supports();
                    assertEquals(2, service.getValue());
                }).thenRunAsync(() -> {
                    // updating a transaction scoped bean outside of a transacction should fail
                    callServiceExpectFailure((tm, svc) -> svc.never(), transactionManager, service);
                    assertEquals(2, service.getValue());
                }).thenRunAsync(() -> {
                    // updating a transaction scoped bean outside of a transacction should fail
                    callServiceExpectFailure((tm, svc) -> svc.notSupported(), transactionManager, service);
                    assertEquals(2, service.getValue());
                }).thenRunAsync(() -> {
                    service.mandatory();
                    // the service call should have updated the bean in this transaction scope
                    assertEquals(3, service.getValue());
                });

                stage0.join();
            } finally {
                // Must end the transaction in the same thread it was started from.
                // If it is ended by an executor thread then the transaction context provider
                // will re-associate the terminated transaction with the initiating thread when
                // the executor finishes.
                try {
                    // the bean increments should have happened
                    assertEquals(3, service.getValue());
                    transactionManager.rollback();

                    try {
                        // since the transactio has finished verify that the transaction scoped bean is no longer bound
                        service.getValue();
                        fail("TransactionScoped bean should only be available from transaction scope");
                    } catch (Exception ignore) {
                    }
                } finally {
                    assertNull(transactionManager.getTransaction(), "transaction still active");
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
                fail("TransactionScoped bean should only be available from transaction scope");
            } catch (Exception ignore) {
                // expected
            } finally {
                tm.resume(transaction);
            }
        } catch (SystemException | InvalidTransactionException e) {
            fail("Unable to suspend or resume transaction");
        }
    }

    @Test
    void concurrentTransactionPropagation() throws Exception {
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
                assertEquals(0, service.getValue());

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
                    assertEquals(2, service.getValue());
                    transactionManager.rollback();

                    try {
                        service.getValue();
                        fail("TransactionScoped bean should only be available from transaction scope");
                    } catch (Exception ignore) {
                    }
                } finally {
                    assertNull(transactionManager.getTransaction(), "transaction still active");
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void runUnderTransactionOfExecutingThread() {
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
                assertTrue(transactionCallable.call(), "The callable did not run in a transaction");
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }

            try {
                transactionManager.rollback();
                if (transactionManager.getTransaction() != null)
                    fail("transaction still active");
            } catch (SystemException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    void transactionWithUT() throws Exception {
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
                assertEquals(0, service.getValue());

                CompletableFuture<Void> stage = executor.runAsync(service::required);
                stage.join();

                try {
                    ut.rollback();
                    assertEquals(ut.getStatus(), Status.STATUS_NO_TRANSACTION, "transaction still active");
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
    void transactionWithSuspend() throws Exception {
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
