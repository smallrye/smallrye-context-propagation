package io.smallrye.context.test.cdi.providedBeans;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.jboss.weld.proxy.WeldClientProxy;

import io.smallrye.context.SmallRyeManagedExecutor;
import io.smallrye.context.SmallRyeThreadContext;

public class Utils {

    private Utils() {
    }

    public static SmallRyeManagedExecutor unwrapExecutor(ManagedExecutor executor) {
        if (executor instanceof WeldClientProxy) {
            return (SmallRyeManagedExecutor) ((WeldClientProxy) executor).getMetadata().getContextualInstance();
        } else {
            throw new IllegalStateException("Injected proxies are expected to be instance of WeldClientProxy");
        }
    }

    public static SmallRyeThreadContext unwrapThreadContext(ThreadContext executor) {
        if (executor instanceof WeldClientProxy) {
            return (SmallRyeThreadContext) ((WeldClientProxy) executor).getMetadata().getContextualInstance();
        } else {
            throw new IllegalStateException("Injected proxies are expected to be instance of WeldClientProxy");
        }
    }

    public static Set<String> providersToStringSet(Set<ThreadContextProvider> providers) {
        Set<String> result = new HashSet<>();
        for (ThreadContextProvider provider : providers) {
            result.add(provider.getThreadContextType());
        }
        return result;
    }
}
