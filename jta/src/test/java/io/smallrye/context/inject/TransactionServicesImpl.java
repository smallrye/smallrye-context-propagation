package io.smallrye.context.inject;

import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import org.jboss.weld.transaction.spi.TransactionServices;

public class TransactionServicesImpl implements TransactionServices {
    private Transaction getTransaction() {
        try {
            TransactionManager tm = com.arjuna.ats.jta.TransactionManager.transactionManager();

            return tm == null ? null : tm.getTransaction();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void registerSynchronization(Synchronization synchronization) {
        Transaction transaction = getTransaction();

        if (transaction == null) {
            throw new RuntimeException("No active transaction");
        }

        try {
            transaction.registerSynchronization(synchronization);
        } catch (RollbackException | SystemException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isTransactionActive() {
        try {
            Transaction transaction = getTransaction();
            int status;

            if (transaction == null) {
                return false;
            }

            status = transaction.getStatus();

            return status == Status.STATUS_ACTIVE ||
                    status == Status.STATUS_COMMITTING ||
                    status == Status.STATUS_MARKED_ROLLBACK ||
                    status == Status.STATUS_PREPARED ||
                    status == Status.STATUS_PREPARING ||
                    status == Status.STATUS_ROLLING_BACK;
        } catch (SystemException | RuntimeException e) {
            return false;
        }
    }

    @Override
    public UserTransaction getUserTransaction() {
        try {
            return com.arjuna.ats.jta.UserTransaction.userTransaction();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void cleanup() {
    }
}
