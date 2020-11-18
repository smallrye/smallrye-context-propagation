package io.smallrye.context.storage;

import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

/**
 * My original plan revolved around having a single ThreadContextProvider for all the contexts we stuff into
 * QuarkusContexts, but this didn't work in practice. Keeping to see if I can salvage it.
 */
class QuarkusStorageThreadContextProvider implements ThreadContextProvider {

    private final ThreadLocal<QuarkusThreadContext> fallbackThreadLocal = new ThreadLocal<QuarkusThreadContext>() {
        @Override
        protected QuarkusThreadContext initialValue() {
            // this is generated somehow
            return new QuarkusThreadContextImpl();
        }
    };

    private QuarkusThreadContext getContexts() {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof QuarkusThread) {
            System.err.println("Capturing context from QT: " + ((QuarkusThread) currentThread).getQuarkusThreadContext());
            return ((QuarkusThread) currentThread).getQuarkusThreadContext();
        } else {
            System.err.println("Capturing context from TL: " + fallbackThreadLocal.get());
            return fallbackThreadLocal.get();
        }
    }

    private QuarkusThreadContext setContexts(QuarkusThreadContext capturedContexts) {
        Thread currentThread = Thread.currentThread();
        QuarkusThreadContext ret;
        if (currentThread instanceof QuarkusThread) {
            System.err.println("Setting context from QT: " + capturedContexts);
            ret = ((QuarkusThread) currentThread).getQuarkusThreadContext();
            ((QuarkusThread) currentThread).setQuarkusThreadContext(capturedContexts);
        } else {
            System.err.println("Setting context from TL: " + capturedContexts);
            ret = fallbackThreadLocal.get();
            fallbackThreadLocal.set(capturedContexts);
        }
        return ret;
    }

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        // need to copy the context value because it's mutable
        QuarkusThreadContext contexts = getContexts().copy();
        return () -> {
            QuarkusThreadContext preservedContexts = setContexts(contexts);
            return () -> setContexts(preservedContexts);
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        QuarkusThreadContext contexts = new QuarkusThreadContextImpl();
        return () -> {
            QuarkusThreadContext preservedContexts = setContexts(contexts);
            return () -> setContexts(preservedContexts);
        };
    }

    @Override
    public String getThreadContextType() {
        return "QuarkusContext";
    }

}