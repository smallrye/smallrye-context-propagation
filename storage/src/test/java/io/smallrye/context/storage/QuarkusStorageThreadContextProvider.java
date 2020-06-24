package io.smallrye.context.storage;

import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

/**
 * My original plan revolved around having a single ThreadContextProvider for all the contexts we stuff into
 * QuarkusContexts, but this didn't work in practice. Keeping to see if I can salvage it.
 */
class QuarkusStorageThreadContextProvider implements ThreadContextProvider {

    private final ThreadLocal<QuarkusContexts> fallbackThreadLocal = new ThreadLocal() {
        @Override
        protected Object initialValue() {
            // this is generated somehow
            return new QuarkusContextsImpl();
        }
    };

    private QuarkusContexts getContexts() {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof QuarkusThread) {
            System.err.println("Capturing context from QT: " + ((QuarkusThread) currentThread).contexts);
            return ((QuarkusThread) currentThread).contexts;
        } else {
            System.err.println("Capturing context from TL: " + fallbackThreadLocal.get());
            return fallbackThreadLocal.get();
        }
    }

    private QuarkusContexts setContexts(QuarkusContexts capturedContexts) {
        Thread currentThread = Thread.currentThread();
        QuarkusContexts ret;
        if (currentThread instanceof QuarkusThread) {
            System.err.println("Setting context from QT: " + capturedContexts);
            ret = ((QuarkusThread) currentThread).contexts;
            ((QuarkusThread) currentThread).contexts = capturedContexts;
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
        QuarkusContexts contexts = getContexts().copy();
        return () -> {
            QuarkusContexts preservedContexts = setContexts(contexts);
            return () -> setContexts(preservedContexts);
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        QuarkusContexts contexts = new QuarkusContextsImpl();
        return () -> {
            QuarkusContexts preservedContexts = setContexts(contexts);
            return () -> setContexts(preservedContexts);
        };
    }

    @Override
    public String getThreadContextType() {
        return "QuarkusContext";
    }

}