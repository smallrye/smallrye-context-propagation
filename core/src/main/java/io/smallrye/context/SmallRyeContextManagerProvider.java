package io.smallrye.context;

import java.util.Collections;
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

    // This is used to speed-up the single classloader use case: it becomes null if elements become > 1
    // causing a fallback to contextManagersForClassLoader
    private volatile Map<ClassLoader, SmallRyeContextManager> singleContextManager = Collections.emptyMap();

    private Map<ClassLoader, SmallRyeContextManager> contextManagersForClassLoader = null;

    @Override
    public SmallRyeContextManager getContextManager() {
        return getContextManager(Thread.currentThread().getContextClassLoader());
    }

    @Override
    public SmallRyeContextManager getContextManager(ClassLoader classLoader) {
        final Map<ClassLoader, SmallRyeContextManager> singleContextManager = this.singleContextManager;
        if (singleContextManager != null) {
            final SmallRyeContextManager config = singleContextManager.get(classLoader);
            if (config != null) {
                return config;
            }
            return guardedCreateSingleContextManager(classLoader);
        }
        return guardedCreateContextManager(classLoader);
    }

    private synchronized SmallRyeContextManager guardedCreateSingleContextManager(ClassLoader classLoader) {
        final Map<ClassLoader, SmallRyeContextManager> singleContextManager = this.singleContextManager;
        if (singleContextManager != null) {
            final SmallRyeContextManager config = singleContextManager.get(classLoader);
            if (config != null) {
                return config;
            }
            return getContextManagerBuilder().forClassLoader(classLoader).registerOnProvider()
                    .addDiscoveredThreadContextProviders().addDiscoveredContextManagerExtensions().build();
        }
        return guardedCreateContextManager(classLoader);
    }

    private synchronized SmallRyeContextManager guardedCreateContextManager(ClassLoader classLoader) {
        final SmallRyeContextManager config = contextManagersForClassLoader.get(classLoader);
        if (config != null) {
            return config;
        }
        return getContextManagerBuilder().forClassLoader(classLoader).registerOnProvider()
                .addDiscoveredThreadContextProviders().addDiscoveredContextManagerExtensions().build();
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
            final Map<ClassLoader, SmallRyeContextManager> singleContextManager = this.singleContextManager;
            if (singleContextManager != null) {
                if (singleContextManager.isEmpty() || singleContextManager.containsKey(manager)) {
                    // can replace the existing one, if any
                    this.singleContextManager = Collections.singletonMap(classLoader, (SmallRyeContextManager) manager);
                } else {
                    // abandon the single ctx manager state
                    this.singleContextManager = null;
                    final Map<ClassLoader, SmallRyeContextManager> contextManagersForClassLoader = new HashMap<>(
                            singleContextManager);
                    contextManagersForClassLoader.put(classLoader, (SmallRyeContextManager) manager);
                    this.contextManagersForClassLoader = contextManagersForClassLoader;
                }
            } else {
                contextManagersForClassLoader.put(classLoader, (SmallRyeContextManager) manager);
            }
        }
    }

    @Override
    public void releaseContextManager(ContextManager manager) {
        synchronized (this) {
            final Map<ClassLoader, SmallRyeContextManager> singleContextManager = this.singleContextManager;
            if (singleContextManager != null) {
                if (!singleContextManager.isEmpty() && singleContextManager.containsValue(manager)) {
                    this.singleContextManager = Collections.emptyMap();
                }
            } else {
                Iterator<Map.Entry<ClassLoader, SmallRyeContextManager>> iterator = contextManagersForClassLoader.entrySet()
                        .iterator();
                while (iterator.hasNext()) {
                    Map.Entry<ClassLoader, SmallRyeContextManager> entry = iterator.next();
                    if (entry.getValue() == manager) {
                        iterator.remove();
                        // we're not compacting to singleContextManager if size is now 1
                        return;
                    }
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
        Map<ClassLoader, SmallRyeContextManager> singleContextManager = this.singleContextManager;
        if (singleContextManager != null) {
            return singleContextManager.get(classLoader);
        }
        synchronized (this) {
            // once singleContextManager is null, it won't EVER become not null again:
            // see releaseContextManager's lack of compaction
            return contextManagersForClassLoader.get(classLoader);
        }
    }

    public static SmallRyeContextManager getManager() {
        return instance().getContextManager();
    }

    public static SmallRyeContextManagerProvider instance() {
        return (SmallRyeContextManagerProvider) ContextManagerProvider.instance();
    }
}
