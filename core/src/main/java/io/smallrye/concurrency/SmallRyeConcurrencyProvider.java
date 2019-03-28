package io.smallrye.concurrency;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.microprofile.context.spi.ContextManager;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;
import org.eclipse.microprofile.context.spi.ContextManagerProviderRegistration;

public class SmallRyeConcurrencyProvider implements ContextManagerProvider {

    private static ContextManagerProviderRegistration registration;

    /**
     * @deprecated Should be removed in favour of SPI
     */
    @Deprecated
    public static void register() {
        if (registration == null)
            registration = ContextManagerProvider.register(new SmallRyeConcurrencyProvider());
    }

    /**
     * @deprecated Should be removed in favour of SPI
     */
    @Deprecated
    public static void unregister() {
        registration.unregister();
        registration = null;
    }

    private Map<ClassLoader, ContextManager> concurrencyManagersForClassLoader = new HashMap<>();

    @Override
    public ContextManager getContextManager() {
        return getContextManager(Thread.currentThread().getContextClassLoader());
    }

    @Override
    public ContextManager getContextManager(ClassLoader classLoader) {
        ContextManager config = concurrencyManagersForClassLoader.get(classLoader);
        if (config == null) {
            synchronized (this) {
                config = concurrencyManagersForClassLoader.get(classLoader);
                if (config == null) {
                    config = getContextManagerBuilder().forClassLoader(classLoader)
                            .addDiscoveredThreadContextProviders().addDiscoveredContextManagerExtensions().build();
                    registerContextManager(config, classLoader);
                }
            }
        }
        return config;
    }

    @Override
    public SmallRyeConcurrencyManagerBuilder getContextManagerBuilder() {
        return new SmallRyeConcurrencyManagerBuilder();
    }

    @Override
    public void registerContextManager(ContextManager manager, ClassLoader classLoader) {
        synchronized (this) {
            concurrencyManagersForClassLoader.put(classLoader, manager);
        }
    }

    @Override
    public void releaseContextManager(ContextManager manager) {
        synchronized (this) {
            Iterator<Map.Entry<ClassLoader, ContextManager>> iterator = concurrencyManagersForClassLoader.entrySet()
                    .iterator();
            while (iterator.hasNext()) {
                Map.Entry<ClassLoader, ContextManager> entry = iterator.next();
                if (entry.getValue() == manager) {
                    iterator.remove();
                    return;
                }
            }
        }
    }

    static public SmallRyeConcurrencyManager getManager() {
        return (SmallRyeConcurrencyManager) ContextManagerProvider.instance().getContextManager();
    }
}
