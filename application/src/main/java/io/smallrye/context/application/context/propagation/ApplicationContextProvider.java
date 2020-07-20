package io.smallrye.context.application.context.propagation;

import java.util.Map;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

public class ApplicationContextProvider implements ThreadContextProvider {

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        ClassLoader capturedTCCL = Thread.currentThread().getContextClassLoader();
        return () -> {
            ClassLoader movedTCCL = Thread.currentThread().getContextClassLoader();
            if (capturedTCCL != movedTCCL)
                Thread.currentThread().setContextClassLoader(capturedTCCL);
            return () -> {
                if (Thread.currentThread().getContextClassLoader() != movedTCCL)
                    Thread.currentThread().setContextClassLoader(movedTCCL);
            };
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        ClassLoader capturedTCCL = null;
        return () -> {
            ClassLoader movedTCCL = Thread.currentThread().getContextClassLoader();
            if (capturedTCCL != movedTCCL)
                Thread.currentThread().setContextClassLoader(capturedTCCL);
            return () -> {
                if (Thread.currentThread().getContextClassLoader() != movedTCCL)
                    Thread.currentThread().setContextClassLoader(movedTCCL);
            };
        };
    }

    @Override
    public String getThreadContextType() {
        return ThreadContext.APPLICATION;
    }
}
