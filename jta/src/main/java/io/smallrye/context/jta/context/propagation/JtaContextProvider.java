package io.smallrye.context.jta.context.propagation;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

public class JtaContextProvider implements ThreadContextProvider {
    private static final Logger logger = Logger.getLogger(JtaContextProvider.class.getName());

    private volatile TransactionManager transactionManager;
    // this allows us to cache null values
    private volatile boolean transactionManagerNotAvailable;
    // cached value indicating if CDI is not available, boolean is assigned on first call
    private AtomicReference<Boolean> cdiNotAvailable = new AtomicReference<>(null);

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        if (isCdiUnavailable()) {
            //return null as per docs to state that propagation of this context is not supported
            return null;
        }
        TransactionManager tm = tm();
        if (tm == null) {
            //return null as per docs to state that propagation of this context is not supported
            return null;
        }
        Transaction capturedTransaction = currentTransaction(tm);
        return () -> {
            // Thread
            // remove/restore current transaction
            Transaction currentTransaction = currentTransaction(tm);
            if (capturedTransaction != null) {
                if (capturedTransaction != currentTransaction) {
                    if (currentTransaction != null)
                        suspendTransaction(tm);
                    resumeTransaction(tm, capturedTransaction);
                } else {
                    // else we're already in the right transaction
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Keeping current transaction " + currentTransaction);
                    }
                }
            } else if (currentTransaction != null) {
                suspendTransaction(tm);
            }
            return () -> {
                // Main
                if (capturedTransaction != null) {
                    if (capturedTransaction != currentTransaction) {
                        suspendTransaction(tm);
                        if (currentTransaction != null)
                            resumeTransaction(tm, currentTransaction);
                    } else {
                        // else we already were in the right transaction
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("Keeping (not restoring) current transaction " + currentTransaction);
                        }
                    }
                } else if (currentTransaction != null) {
                    resumeTransaction(tm, currentTransaction);
                }
            };
        };
    }

    private void resumeTransaction(TransactionManager tm, Transaction transaction) {
        try {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Resuming transaction " + transaction);
            }
            tm.resume(transaction);
        } catch (InvalidTransactionException | IllegalStateException | SystemException e) {
            throw new RuntimeException(e);
        }
    }

    private void suspendTransaction(TransactionManager tm) {
        try {
            Transaction t = tm.suspend();
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Suspending transaction " + t);
            }
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    private Transaction currentTransaction(TransactionManager tm) {
        try {
            return tm.getTransaction();
        } catch (SystemException e) {
            logger.log(Level.SEVERE, "Failed to capture current transaction", e);
            return null;
        }
    }

    private TransactionManager tm() {
        TransactionManager tm = this.transactionManager;
        // this allows us to cache null values
        if (tm != null || transactionManagerNotAvailable) {
            return tm;
        }
        //no need to guard against double assignment
        Instance<LifecycleManager> lifecycleManagers = CDI.current().select(LifecycleManager.class);
        if (lifecycleManagers.isResolvable()) {
            lifecycleManagers.get().setProvider(this);
        }
        // make sure we handle the case of SR-CP-JTA being imported while Narayana is excluded somehow
        Instance<TransactionManager> tmInstance = CDI.current().select(TransactionManager.class);
        if (tmInstance.isResolvable()) {
            tm = tmInstance.get();
            // so in theory we can't get null here, but better safe than sorry
            if (tm != null) {
                // validate it, because it can be an empty proxy
                // Here Matej says it can't be an empty proxy, but even though I don't have tests
                // for this, this looks too specific to have come up by accident.
                try {
                    tm.getStatus();
                } catch (CreationException | SystemException x) {
                    // not really there
                    transactionManagerNotAvailable = true;
                    return null;
                }
            }
        } else {
            transactionManagerNotAvailable = true;
            return null;
        }
        // save it for later
        return this.transactionManager = tm;
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        if (isCdiUnavailable()) {
            //return null as per docs to state that propagation of this context is not supported
            return null;
        }

        TransactionManager tm = tm();
        if (tm == null) {
            //return null as per docs to state that propagation of this context is not supported
            return null;
        }
        return () -> {
            // remove/restore current transaction
            Transaction currentTransaction = currentTransaction(tm);
            if (currentTransaction != null) {
                suspendTransaction(tm);
            }
            return () -> {
                if (currentTransaction != null) {
                    resumeTransaction(tm, currentTransaction);
                }
            };
        };
    }

    @Override
    public String getThreadContextType() {
        return ThreadContext.TRANSACTION;
    }

    /**
     * Checks if CDI is available within the application by using {@code CDI.current()}.
     * If an exception is thrown, it is suppressed and false is returns, otherwise true is returned.
     * <p/>
     * Uses {@code CDI.current()} in its first invocation; subsequent invocations use cached value as we presume the
     * presence/absence state of CDI within application cannot change while the application executes. This should
     * improve performance.
     * <p/>
     * NOTE: this has a potential downside if the CDI provider exists but the CDI container can change its state while user
     * application executes. I.e. manually stopping the CDI container and then trying to use JTA context propagation
     * will behave weirdly.
     *
     * @return true if CDI can be used, false otherwise
     */
    private boolean isCdiUnavailable() {
        Boolean cdiUnavailable = cdiNotAvailable.get();
        if (cdiUnavailable != null) {
            return cdiUnavailable;
        }
        if (transactionManager != null) {
            //we looked this up from CDI, so we know it is fine
            cdiNotAvailable.set(false);
            return false;
        }
        try {
            boolean result = CDI.current() == null;
            cdiNotAvailable.set(result);
            return result;
        } catch (IllegalStateException e) {
            // no CDI provider found, CDI isn't available
            cdiNotAvailable.set(true);
            return true;
        }
    }

    /**
     * bean used to clear the cached TM when the container shuts down
     */
    @ApplicationScoped
    public static class LifecycleManager {

        private volatile JtaContextProvider provider;

        public JtaContextProvider getProvider() {
            return provider;
        }

        public LifecycleManager setProvider(JtaContextProvider provider) {
            this.provider = provider;
            return this;
        }

        @PreDestroy
        void shutdown() {
            provider.transactionManager = null;
            provider.transactionManagerNotAvailable = false;
        }
    }
}
