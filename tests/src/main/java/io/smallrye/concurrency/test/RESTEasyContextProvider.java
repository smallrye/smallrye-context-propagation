package io.smallrye.concurrency.test;

import java.util.Map;

import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;
import org.eclipse.microprofile.concurrent.spi.ThreadContextSnapshot;
import org.jboss.resteasy.core.ResteasyContext;

public class RESTEasyContextProvider implements ThreadContextProvider {

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        Map<Class<?>, Object> capturedContext = ResteasyContext.getContextDataMap();
        return () -> {
            ResteasyContext.pushContextDataMap(capturedContext);
            return () -> {
                ResteasyContext.removeContextDataLevel();
            };
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return () -> {
            Map<Class<?>, Object> movedContext = ResteasyContext.getContextDataMap();
            ResteasyContext.clearContextData();
            return () -> {
                if (movedContext == null)
                    ResteasyContext.clearContextData();
                else
                    ResteasyContext.pushContextDataMap(movedContext);
            };
        };
    }

    @Override
    public String getThreadContextType() {
        return "RESTEasyContext";
    }

}
