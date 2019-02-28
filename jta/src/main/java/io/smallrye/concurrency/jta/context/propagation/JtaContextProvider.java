package io.smallrye.concurrency.jta.context.propagation;

import org.eclipse.microprofile.concurrent.ThreadContext;
import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;
import org.eclipse.microprofile.concurrent.spi.ThreadContextSnapshot;

import javax.enterprise.inject.spi.CDI;
import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.util.Map;
import java.util.logging.Logger;

public class JtaContextProvider implements ThreadContextProvider {
    private static final Logger logger = Logger.getLogger(JtaContextProvider.class.getName());

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        if (isCdiUnavailable()) {
            //return null as per docs to state that propagation of this context is not supported
            return null;
        }

        Transaction capturedTransaction = currentTransaction();
        return () -> {
            // remove/restore current transaction
            Transaction currentTransaction = currentTransaction();
            if(capturedTransaction != null) {
                if(capturedTransaction != currentTransaction) {
                    if(currentTransaction != null)
                        suspendTransaction();
                    resumeTransaction(capturedTransaction);
                }else{
                    // else we're already in the right transaction
                    logger.fine("Keeping current transaction "+currentTransaction);
                }
            } else if(currentTransaction != null) {
                suspendTransaction();
            }
            return () -> {
                if(capturedTransaction != null) {
                    if(capturedTransaction != currentTransaction) {
                        suspendTransaction();
                        if(currentTransaction != null)
                            resumeTransaction(currentTransaction);
                    }else{
                        // else we already were in the right transaction
                        logger.fine("Keeping (not restoring) current transaction "+currentTransaction);
                    }
                } else if(currentTransaction != null) {
                    resumeTransaction(currentTransaction);
                }
            };
        };
    }

    private void resumeTransaction(Transaction transaction) {
        try {
            logger.fine("Resuming transaction "+transaction);
            tm().resume(transaction);
        } catch (InvalidTransactionException | IllegalStateException | SystemException e) {
            throw new RuntimeException(e);
        }
    }

    private void suspendTransaction() {
        try {
            Transaction t = tm().suspend();
            logger.fine("Suspending transaction "+t);
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    private Transaction currentTransaction() {
        try {
            return tm().getTransaction();
        } catch (SystemException e) {
            e.printStackTrace();
            return null;
        }
    }

    private TransactionManager tm() {
        return CDI.current().select(TransactionManager.class).get();
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        if (isCdiUnavailable()) {
            //return null as per docs to state that propagation of this context is not supported
            return null;
        }

        return () -> {
            // remove/restore current transaction
            Transaction currentTransaction = currentTransaction();
            if(currentTransaction != null) {
                suspendTransaction();
            }
            return () -> {
                if(currentTransaction != null) {
                    resumeTransaction(currentTransaction);
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
     *
     * @return true if CDI can be used, false otherwise
     */
    private boolean isCdiUnavailable() {
        try {
            return CDI.current() == null;
        } catch (IllegalStateException e) {
            // no CDI provider found, CDI isn't available
            return true;
        }
    }
}
