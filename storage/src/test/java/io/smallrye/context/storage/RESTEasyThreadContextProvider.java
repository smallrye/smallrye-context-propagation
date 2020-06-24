package io.smallrye.context.storage;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

/**
 * Copy of RESTEasy's ThreadContextProvider for test purposes
 */
class RESTEasyThreadContextProvider implements ThreadContextProvider {

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        Map<Class<?>, Object> capturedContext = RESTEasyContext.getContext();
        return () -> {
            CloseableContext context = RESTEasyContext.pushContextLevelProper(capturedContext);
            return () -> context.close();
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        Map<Class<?>, Object> capturedContext = new HashMap<>();
        return () -> {
            CloseableContext context = RESTEasyContext.pushContextLevelProper(capturedContext);
            return () -> context.close();
        };
    }

    @Override
    public String getThreadContextType() {
        return "RESTEasy";
    }

}