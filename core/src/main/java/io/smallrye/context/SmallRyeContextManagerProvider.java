package io.smallrye.context;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.microprofile.context.spi.ContextManager;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;
import org.eclipse.microprofile.context.spi.ContextManagerProviderRegistration;

public class SmallRyeContextManagerProvider implements ContextManagerProvider {

    private static ContextManagerProviderRegistration registration;

    /**
     * @deprecated Should be removed in favour of SPI
     */
    @Deprecated
    public static void register() {
        if (registration == null)
            registration = ContextManagerProvider.register(new SmallRyeContextManagerProvider());
    }

    /**
     * @deprecated Should be removed in favour of SPI
     */
    @Deprecated
    public static void unregister() {
        registration.unregister();
        registration = null;
    }

    private Map<ClassLoader, ContextManager> contextManagersForClassLoader = new HashMap<>();

    @Override
    public ContextManager getContextManager() {
        return getContextManager(Thread.currentThread().getContextClassLoader());
    }

    @Override
    public ContextManager getContextManager(ClassLoader classLoader) {
        ContextManager config = contextManagersForClassLoader.get(classLoader);
        if (config == null) {
            synchronized (this) {
                config = contextManagersForClassLoader.get(classLoader);
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
    public SmallRyeContextManager.Builder getContextManagerBuilder() {
        return new SmallRyeContextManager.Builder();
    }

    @Override
    public void registerContextManager(ContextManager manager, ClassLoader classLoader) {
        synchronized (this) {
            contextManagersForClassLoader.put(classLoader, manager);
        }
    }

    @Override
    public void releaseContextManager(ContextManager manager) {
        synchronized (this) {
            Iterator<Map.Entry<ClassLoader, ContextManager>> iterator = contextManagersForClassLoader.entrySet()
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

    static public SmallRyeContextManager getManager() {
        return (SmallRyeContextManager) ContextManagerProvider.instance().getContextManager();
    }
}
