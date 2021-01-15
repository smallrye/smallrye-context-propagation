package io.smallrye.context;

import java.util.Map;

import io.smallrye.context.storage.spi.StorageDeclaration;
import io.smallrye.context.storage.spi.StorageManager;

public interface FastStorageThreadContextProvider<Declaration extends StorageDeclaration<?>> extends FastThreadContextProvider {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public default ThreadLocal<?> threadLocal(Map<String, String> props) {
        return StorageManager.threadLocal((Class) getStorageDeclaration());
    }

    Class<Declaration> getStorageDeclaration();
}
