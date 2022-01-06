package io.smallrye.context.impl.cdi;

import java.util.Collection;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.eclipse.microprofile.context.ThreadContext;

import io.smallrye.context.CleanAutoCloseable;
import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.context.api.CurrentThreadContext;

@CurrentThreadContext
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class SmallRyeCurrentThreadContextInterceptor {

    @AroundInvoke
    public Object manageCurrentContext(InvocationContext ctx) throws Exception {
        CurrentThreadContext config = null;
        Object binding = ctx.getContextData().get("io.quarkus.arc.interceptorBindings");
        if (binding == null)
            binding = ctx.getContextData().get("org.jboss.weld.interceptor.bindings");
        if (binding instanceof Collection) {
            for (Object b : (Collection<?>) binding) {
                if (b instanceof CurrentThreadContext) {
                    config = (CurrentThreadContext) b;
                    break;
                }
            }
        }
        if (config == null && ctx.getMethod() != null)
            config = ctx.getMethod().getAnnotation(CurrentThreadContext.class);
        if (config != null) {
            final String[] unchanged = config.unchanged();
            final String[] propagated = config.propagated();
            final String[] cleared = config.cleared();
            if (propagated.length == 0 &&
                    cleared.length == 0 &&
                    unchanged.length == 1 && ThreadContext.ALL_REMAINING.equals(unchanged[0])) {
                //Skip processing of Context Propagation in this case:
                return ctx.proceed();
            } else {
                SmallRyeThreadContext newTC = config.remove()
                        ? null
                        : SmallRyeThreadContext.builder()
                                .cleared(cleared)
                                .propagated(propagated)
                                .unchanged(unchanged)
                                .build();
                try (CleanAutoCloseable ac = SmallRyeThreadContext.withThreadContext(newTC)) {
                    return ctx.proceed();
                }
            }
        } else {
            // could not find any config, that's an error, but we can continue
            return ctx.proceed();
        }
    }
}
