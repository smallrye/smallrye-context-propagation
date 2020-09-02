package io.smallrye.context.inject;

import java.util.Collection;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import io.smallrye.context.CleanAutoCloseable;
import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.context.api.CurrentThreadContext;

@CurrentThreadContext
@Interceptor
@Priority(0)
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
            SmallRyeThreadContext newTC = config.remove()
                    ? null
                    : SmallRyeThreadContext.builder()
                            .cleared(config.cleared())
                            .propagated(config.propagated())
                            .unchanged(config.unchanged())
                            .build();
            try (CleanAutoCloseable ac = SmallRyeThreadContext.withThreadContext(newTC)) {
                return ctx.proceed();
            }
        } else {
            // could not find any config, that's an error, but we can continue
            return ctx.proceed();
        }
    }
}
