package io.smallrye.context;

import java.util.Map;

import io.smallrye.context.storage.spi.StorageDeclaration;
import io.smallrye.context.storage.spi.StorageManager;

/**
 * Special implementation of a {@link FastThreadContextProvider} if your context is using {@link StorageManager} to obtain its
 * ThreadLocal, in which case we can obtain it from there to propagate it, if we know its {@link StorageDeclaration}.
 * 
 * @param <Declaration> The StorageDeclaration for that ThreadLocal
 */
public interface FastStorageThreadContextProvider<Declaration extends StorageDeclaration<?>> extends FastThreadContextProvider {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public default ThreadLocal<?> threadLocal(Map<String, String> props) {
        return StorageManager.threadLocal((Class) getStorageDeclaration());
    }

    /**
     * @return the {@link StorageDeclaration} to use to obtain the ThreadLocal to propagate.
     */
    Class<Declaration> getStorageDeclaration();
}
