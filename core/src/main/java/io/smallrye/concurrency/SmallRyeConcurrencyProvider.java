package io.smallrye.concurrency;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.microprofile.concurrent.spi.ConcurrencyManager;
import org.eclipse.microprofile.concurrent.spi.ConcurrencyManagerBuilder;
import org.eclipse.microprofile.concurrent.spi.ConcurrencyProvider;
import org.eclipse.microprofile.concurrent.spi.ConcurrencyProviderRegistration;

public class SmallRyeConcurrencyProvider implements ConcurrencyProvider {

	private static ConcurrencyProviderRegistration registration;

	/**
	 * @deprecated Should be removed in favour of SPI
	 */
	@Deprecated
	public static void register() {
		if(registration == null)
			registration = ConcurrencyProvider.register(new SmallRyeConcurrencyProvider());
	}
	
	/**
	 * @deprecated Should be removed in favour of SPI
	 */
	@Deprecated
	public static void unregister() {
		registration.unregister();
		registration = null;
	}

    private Map<ClassLoader,ConcurrencyManager> concurrencyManagersForClassLoader = new HashMap<>();

	@Override
	public ConcurrencyManager getConcurrencyManager() {
		return getConcurrencyManager(Thread.currentThread().getContextClassLoader());
	}

	@Override
	public ConcurrencyManager getConcurrencyManager(ClassLoader classLoader) {
		ConcurrencyManager config = concurrencyManagersForClassLoader.get(classLoader);
        if (config == null) {
            synchronized (this) {
                config = concurrencyManagersForClassLoader.get(classLoader);
                if (config == null) {
                    config = getConcurrencyManagerBuilder()
                    		.forClassLoader(classLoader)
                            .addDiscoveredThreadContextProviders()
                            .addDiscoveredThreadContextPropagators()
                            .build();
                    registerConcurrencyManager(config, classLoader);
                }
            }
        }
        return config;
	}

	@Override
	public SmallRyeConcurrencyManagerBuilder getConcurrencyManagerBuilder() {
		return new SmallRyeConcurrencyManagerBuilder();
	}

	@Override
	public void registerConcurrencyManager(ConcurrencyManager manager, ClassLoader classLoader) {
        synchronized (this) {
            concurrencyManagersForClassLoader.put(classLoader, manager);
        }
	}

	@Override
	public void releaseConcurrencyManager(ConcurrencyManager manager) {
        synchronized (this) {
            Iterator<Map.Entry<ClassLoader, ConcurrencyManager>> iterator = concurrencyManagersForClassLoader.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<ClassLoader, ConcurrencyManager> entry = iterator.next();
                if (entry.getValue() == manager) {
                    iterator.remove();
                    return;
                }
            }
        }
	}

	static public SmallRyeConcurrencyManager getManager() {
		return (SmallRyeConcurrencyManager) ConcurrencyProvider.instance().getConcurrencyManager();
	}
}
