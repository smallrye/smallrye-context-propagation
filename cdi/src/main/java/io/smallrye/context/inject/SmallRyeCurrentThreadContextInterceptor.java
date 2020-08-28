package io.smallrye.context.inject;

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
        CurrentThreadContext config = ctx.getMethod().getAnnotation(CurrentThreadContext.class);
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
    }
}
