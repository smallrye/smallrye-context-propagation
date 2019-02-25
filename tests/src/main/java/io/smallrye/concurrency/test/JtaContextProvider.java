package io.smallrye.concurrency.test;

import java.util.Map;

import javax.enterprise.inject.spi.CDI;
import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;
import org.eclipse.microprofile.concurrent.spi.ThreadContextSnapshot;

public class JtaContextProvider implements ThreadContextProvider {

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
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
                    System.err.println("Keeping current transaction "+currentTransaction);
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
                        System.err.println("Keeping (not restoring) current transaction "+currentTransaction);
                    }
                } else if(currentTransaction != null) {
                    resumeTransaction(currentTransaction);
                }
            };
        };
    }

    private void resumeTransaction(Transaction transaction) {
        try {
            System.err.println("Resuming transaction "+transaction);
            tm().resume(transaction);
        } catch (InvalidTransactionException | IllegalStateException | SystemException e) {
            throw new RuntimeException(e);
        }
    }

    private Transaction suspendTransaction() {
        try {
            Transaction t = tm().suspend();
            System.err.println("Suspending transaction "+t);
            return t;
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
        return "JtaContext";
    }

}
