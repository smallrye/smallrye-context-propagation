package io.smallrye.context.test;

import org.jnp.server.NamingBeanImpl;

import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.jta.utils.JNDIManager;

public class JTAUtils {
    private static final String txnStoreLocation = "target/tx-object-store";
    private static NamingBeanImpl namingBean;

    public static void startJTATM() {
        try {
            // Start a JNDI server
            namingBean = new NamingBeanImpl();
            namingBean.start();

            // Bind the JTA implementation to the correct JNDI contexts
            JNDIManager.bindJTAImplementation();

            // Set object store location
            arjPropertyManager.getObjectStoreEnvironmentBean().setObjectStoreDir(txnStoreLocation);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void stop() {
        namingBean.stop();
    }
}
