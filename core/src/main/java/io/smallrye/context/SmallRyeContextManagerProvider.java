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

    private Map<ClassLoader, SmallRyeContextManager> contextManagersForClassLoader = new HashMap<>();

    @Override
    public SmallRyeContextManager getContextManager() {
        return getContextManager(Thread.currentThread().getContextClassLoader());
    }

    @Override
    public SmallRyeContextManager getContextManager(ClassLoader classLoader) {
        SmallRyeContextManager config = contextManagersForClassLoader.get(classLoader);
        if (config == null) {
            synchronized (this) {
                config = contextManagersForClassLoader.get(classLoader);
                if (config == null) {
                    config = getContextManagerBuilder().forClassLoader(classLoader).registerOnProvider()
                            .addDiscoveredThreadContextProviders().addDiscoveredContextManagerExtensions().build();
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
        if (manager instanceof SmallRyeContextManager == false) {
            throw new IllegalArgumentException("Only instances of SmallRyeContextManager are supported: " + manager);
        }
        synchronized (this) {
            contextManagersForClassLoader.put(classLoader, (SmallRyeContextManager) manager);
        }
    }

    @Override
    public void releaseContextManager(ContextManager manager) {
        synchronized (this) {
            Iterator<Map.Entry<ClassLoader, SmallRyeContextManager>> iterator = contextManagersForClassLoader.entrySet()
                    .iterator();
            while (iterator.hasNext()) {
                Map.Entry<ClassLoader, SmallRyeContextManager> entry = iterator.next();
                if (entry.getValue() == manager) {
                    iterator.remove();
                    return;
                }
            }
        }
    }

    /**
     * Looks for any context manager registered for a particular class loader. If not found
     * it will not throw an error, unlike {@link #getContextManager(ClassLoader)}.
     *
     * @param classLoader The class loader
     * @return The context manager for the class loader, or {@code null} if none could be found
     */
    public ContextManager findContextManager(ClassLoader classLoader) {
        return contextManagersForClassLoader.get(classLoader);
    }

    public static SmallRyeContextManager getManager() {
        return instance().getContextManager();
    }

    public static SmallRyeContextManagerProvider instance() {
        return (SmallRyeContextManagerProvider) ContextManagerProvider.instance();
    }
}
