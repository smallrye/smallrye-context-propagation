package io.smallrye.concurrency.test.jta;
import com.arjuna.ats.jta.UserTransaction;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

@Dependent
public class UserTransactionProducer {
    @Produces
    @ApplicationScoped
    public javax.transaction.UserTransaction userTransaction() {
        return UserTransaction.userTransaction();
    }
}
