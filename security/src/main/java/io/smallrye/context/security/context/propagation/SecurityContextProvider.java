package io.smallrye.context.security.context.propagation;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.concurrent.Callable;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.smallrye.context.spi.WrappingThreadContextSnapshot;

public class SecurityContextProvider implements ThreadContextProvider {

    private static final ThreadContextController NOOP_THREAD_CONTEXT_CONTROLLER = new ThreadContextController() {
        @Override
        public void endContext() throws IllegalStateException {
        }
    };

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        return new SecurityThreadContextSnapshot(true);
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return new SecurityThreadContextSnapshot(false);
    }

    @Override
    public String getThreadContextType() {
        return ThreadContext.SECURITY;
    }

    static final class SecurityThreadContextSnapshot implements WrappingThreadContextSnapshot {

        private final AccessControlContext accessControlContext;

        SecurityThreadContextSnapshot(final boolean capture) {
            if (capture) {
                accessControlContext = AccessController.getContext();
            } else {
                accessControlContext = null;
            }
        }

        @Override
        public ThreadContextController begin() {
            return NOOP_THREAD_CONTEXT_CONTROLLER;
        }

        @Override
        public boolean needsToWrap() {
            return accessControlContext != null;
        }

        @Override
        public <T> Callable<T> wrap(Callable<T> callable) {
            if (accessControlContext == null) {
                return callable;
            }

            return new Callable<T>() {

                @Override
                public T call() throws Exception {
                    try {
                        return AccessController.doPrivilegedWithCombiner((PrivilegedExceptionAction<T>) callable::call,
                                accessControlContext);
                    } catch (PrivilegedActionException e) {
                        throw e.getException();
                    }
                }
            };
        }

    }

}
