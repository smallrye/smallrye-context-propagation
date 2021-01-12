package io.smallrye.context.application.context.propagation;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

public class ApplicationContextProvider implements ThreadContextProvider {

    static final ClassLoader SYSTEM_CL;

    static {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            SYSTEM_CL = AccessController
                    .doPrivileged((PrivilegedAction<ClassLoader>) ApplicationContextProvider::calculateSystemClassLoader);
        } else {
            SYSTEM_CL = calculateSystemClassLoader();
        }
    }

    private static ClassLoader calculateSystemClassLoader() {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        if (cl == null) {
            // non-null ref that delegates to the system
            cl = new ClassLoader(null) {
            };
        }
        return cl;
    }

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        ClassLoader capturedTCCL = Thread.currentThread().getContextClassLoader();
        return new ThreadContextSnapshot() {
            @Override
            public ThreadContextController begin() {
                ClassLoader movedTCCL = Thread.currentThread().getContextClassLoader();
                if (capturedTCCL != movedTCCL)
                    Thread.currentThread().setContextClassLoader(capturedTCCL);
                return new ThreadContextController() {
                    @Override
                    public void endContext() throws IllegalStateException {
                        if (Thread.currentThread().getContextClassLoader() != movedTCCL)
                            Thread.currentThread().setContextClassLoader(movedTCCL);
                    }
                };
            }
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        ClassLoader capturedTCCL = SYSTEM_CL;
        return new ThreadContextSnapshot() {
            @Override
            public ThreadContextController begin() {
                ClassLoader movedTCCL = Thread.currentThread().getContextClassLoader();
                if (capturedTCCL != movedTCCL)
                    Thread.currentThread().setContextClassLoader(capturedTCCL);
                return new ThreadContextController() {
                    @Override
                    public void endContext() throws IllegalStateException {
                        if (Thread.currentThread().getContextClassLoader() != movedTCCL)
                            Thread.currentThread().setContextClassLoader(movedTCCL);
                    }
                };
            }
        };
    }

    @Override
    public String getThreadContextType() {
        return ThreadContext.APPLICATION;
    }
}
