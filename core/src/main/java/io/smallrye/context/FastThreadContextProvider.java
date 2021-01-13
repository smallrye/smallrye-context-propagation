package io.smallrye.context;

import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;

public interface FastThreadContextProvider extends ThreadContextProvider {

    ThreadLocal<?> threadLocal(Map<String, String> props);

    Object clearedValue(Map<String, String> props);

}
