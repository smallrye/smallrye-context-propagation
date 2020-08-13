package io.smallrye.context;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;

import io.smallrye.context.impl.ActiveContextState;
import io.smallrye.context.impl.CapturedContextState;
import io.smallrye.context.impl.Contextualized;
import io.smallrye.context.impl.DefaultValues;
import io.smallrye.context.impl.ThreadContextProviderPlan;

public class SmallRyeThreadContext implements ThreadContext {

    private final class ContextualSupplier<R> implements Supplier<R>, Contextualized {
        private final CapturedContextState state;
        private final Supplier<R> supplier;

        private ContextualSupplier(CapturedContextState state, Supplier<R> supplier) {
            this.state = state;
            this.supplier = supplier;
        }

        @Override
        public R get() {
            ActiveContextState activeState = state.begin();
            try {
                return supplier.get();
            } finally {
                activeState.endContext();
            }
        }
    }

    private final class ContextualRunnable implements Runnable, Contextualized {
        private final Runnable runnable;
        private final CapturedContextState state;

        private ContextualRunnable(Runnable runnable, CapturedContextState state) {
            this.runnable = runnable;
            this.state = state;
        }

        @Override
        public void run() {
            ActiveContextState activeState = state.begin();
            try {
                runnable.run();
            } finally {
                activeState.endContext();
            }
        }
    }

    private final class ContextualFunction<T, R> implements Function<T, R>, Contextualized {
        private final CapturedContextState state;
        private final Function<T, R> function;

        private ContextualFunction(CapturedContextState state, Function<T, R> function) {
            this.state = state;
            this.function = function;
        }

        @Override
        public R apply(T t) {
            ActiveContextState activeState = state.begin();
            try {
                return function.apply(t);
            } finally {
                activeState.endContext();
            }
        }
    }

    private final class ContextualConsumer<T> implements Consumer<T>, Contextualized {
        private final CapturedContextState state;
        private final Consumer<T> consumer;

        private ContextualConsumer(CapturedContextState state, Consumer<T> consumer) {
            this.state = state;
            this.consumer = consumer;
        }

        @Override
        public void accept(T t) {
            ActiveContextState activeState = state.begin();
            try {
                consumer.accept(t);
            } finally {
                activeState.endContext();
            }
        }
    }

    private final class ContextualCallable<R> implements Callable<R>, Contextualized {
        private final CapturedContextState state;
        private final Callable<R> callable;

        private ContextualCallable(CapturedContextState state, Callable<R> callable) {
            this.state = state;
            this.callable = callable;
        }

        @Override
        public R call() throws Exception {
            ActiveContextState activeState = state.begin();
            try {
                return callable.call();
            } finally {
                activeState.endContext();
            }
        }
    }

    private final class ContextualBiFunction<T, U, R> implements BiFunction<T, U, R>, Contextualized {
        private final CapturedContextState state;
        private final BiFunction<T, U, R> function;

        private ContextualBiFunction(CapturedContextState state, BiFunction<T, U, R> function) {
            this.state = state;
            this.function = function;
        }

        @Override
        public R apply(T t, U u) {
            ActiveContextState activeState = state.begin();
            try {
                return function.apply(t, u);
            } finally {
                activeState.endContext();
            }
        }
    }

    private final class ContextualBiConsumer<T, U> implements BiConsumer<T, U>, Contextualized {
        private final BiConsumer<T, U> consumer;
        private final CapturedContextState state;

        private ContextualBiConsumer(BiConsumer<T, U> consumer, CapturedContextState state) {
            this.consumer = consumer;
            this.state = state;
        }

        @Override
        public void accept(T t, U u) {
            ActiveContextState activeState = state.begin();
            try {
                consumer.accept(t, u);
            } finally {
                activeState.endContext();
            }
        }
    }

    private final SmallRyeContextManager manager;
    private final ThreadContextProviderPlan plan;
    private final String injectionPointName;
    private final ExecutorService defaultExecutor;

    /**
     * DO NOT USE: for CDI.
     */
    SmallRyeThreadContext() {
        this.manager = null;
        this.plan = null;
        this.injectionPointName = null;
        this.defaultExecutor = null;
    }

    public SmallRyeThreadContext(SmallRyeContextManager manager, String[] propagated, String[] unchanged,
            String[] cleared, String injectionPointName, ExecutorService defaultExecutor) {
        this.manager = manager;
        this.plan = manager.getProviderPlan(propagated, unchanged, cleared);
        this.injectionPointName = injectionPointName;
        this.defaultExecutor = defaultExecutor;
    }

    private void checkPrecontextualized(Object action) {
        if (action instanceof Contextualized)
            throw new IllegalArgumentException("Action is already contextualized");
    }

    public ThreadContextProviderPlan getPlan() {
        return plan;
    }

    //
    // Extras

    public ExecutorService getDefaultExecutor() {
        return defaultExecutor;
    }

    public static Builder builder() {
        return SmallRyeContextManagerProvider.instance().getContextManager().newThreadContextBuilder();
    }

    //
    // Wrappers

    /**
     * <p>
     * Returns a new <code>CompletableFuture</code> that is completed by the completion of the
     * specified stage.
     * </p>
     *
     * <p>
     * The new completable future will use the same default executor as this ThreadContext,
     * which can be a ManagedExecutor if this ThreadContext was obtained by {@link SmallRyeManagedExecutor#getThreadContext()}
     * or the default executor service as set by
     * {@link SmallRyeContextManager.Builder#withDefaultExecutorService(ExecutorService)},
     * or otherwise have no default executor.
     * 
     * If this thread context has no default executor, the new stage and all dependent stages created from it, and so forth,
     * have no default asynchronous execution facility and must raise {@link java.lang.UnsupportedOperationException}
     * for all <code>*Async</code> methods that do not specify an executor. For example,
     * {@link java.util.concurrent.CompletionStage#thenRunAsync(Runnable) thenRunAsync(Runnable)}.
     * </p>
     *
     * <p>
     * When dependent stages are created from the new completable future, thread context is captured
     * and/or cleared as described in the documentation of the {@link ManagedExecutor} class, except that
     * this ThreadContext instance takes the place of the default asynchronous execution facility in
     * supplying the configuration of cleared/propagated context types. This guarantees that the action
     * performed by each stage always runs under the thread context of the code that creates the stage,
     * unless the user explicitly overrides by supplying a pre-contextualized action.
     * </p>
     *
     * <p>
     * Invocation of this method does not impact thread context propagation for the supplied
     * completable future or any dependent stages created from it, other than the new dependent
     * completable future that is created by this method.
     * </p>
     *
     * @param <T> completable future result type.
     * @param future a completable future whose completion triggers completion of the new completable
     *        future that is created by this method.
     * @return the new completable future.
     */
    @Override
    public <T> CompletableFuture<T> withContextCapture(CompletableFuture<T> future) {
        return withContextCapture(future, defaultExecutor);
    }

    public <T> CompletableFuture<T> withContextCapture(CompletableFuture<T> future, Executor executor) {
        return JdkSpecific.newCompletableFutureWrapper(this, future, executor, false);
    }

    /**
     * <p>
     * Returns a new <code>CompletionStage</code> that is completed by the completion of the
     * specified stage.
     * </p>
     *
     * <p>
     * The new completion stage will use the same default executor as this ThreadContext,
     * which can be a ManagedExecutor if this ThreadContext was obtained by {@link SmallRyeManagedExecutor#getThreadContext()}
     * or the default executor service as set by
     * {@link SmallRyeContextManager.Builder#withDefaultExecutorService(ExecutorService)},
     * or otherwise have no default executor.
     * 
     * If this thread context has no default executor, the new stage and all dependent stages created from it, and so forth,
     * and/or cleared as described in the documentation of the {@link ManagedExecutor} class, except that
     * this ThreadContext instance takes the place of the default asynchronous execution facility in
     * supplying the configuration of cleared/propagated context types. This guarantees that the action
     * performed by each stage always runs under the thread context of the code that creates the stage,
     * unless the user explicitly overrides by supplying a pre-contextualized action.
     * </p>
     *
     * <p>
     * Invocation of this method does not impact thread context propagation for the supplied
     * stage or any dependent stages created from it, other than the new dependent
     * completion stage that is created by this method.
     * </p>
     *
     * @param <T> completion stage result type.
     * @param stage a completion stage whose completion triggers completion of the new stage
     *        that is created by this method.
     * @return the new completion stage.
     */
    @Override
    public <T> CompletionStage<T> withContextCapture(CompletionStage<T> stage) {
        return withContextCapture(stage, defaultExecutor);
    }

    public <T> CompletionStage<T> withContextCapture(CompletionStage<T> stage, Executor executor) {
        if (stage instanceof CompletableFuture)
            // the MP-CP TCK insists we cannot complete instances returned by this API
            return JdkSpecific.newCompletableFutureWrapper(this, (CompletableFuture<T>) stage, executor, true);
        return JdkSpecific.newCompletionStageWrapper(this, stage, executor);
    }

    @Override
    public Executor currentContextExecutor() {
        return withContext(manager.captureContext(plan));
    }

    Executor withContext(CapturedContextState state) {
        return (runnable) -> {
            ActiveContextState activeState = state.begin();
            try {
                runnable.run();
            } finally {
                activeState.endContext();
            }
        };
    }

    @Override
    public <T, U> BiConsumer<T, U> contextualConsumer(BiConsumer<T, U> consumer) {
        return contextualConsumer(manager.captureContext(plan), consumer);
    }

    <T, U> BiConsumer<T, U> contextualConsumerUnlessContextualized(BiConsumer<T, U> consumer) {
        if (consumer instanceof Contextualized)
            return consumer;
        return contextualConsumer(consumer);
    }

    <T, U> BiConsumer<T, U> contextualConsumer(CapturedContextState state, BiConsumer<T, U> consumer) {
        checkPrecontextualized(consumer);
        return new ContextualBiConsumer<T, U>(consumer, state);
    }

    @Override
    public <T, U, R> BiFunction<T, U, R> contextualFunction(BiFunction<T, U, R> function) {
        return contextualFunction(manager.captureContext(plan), function);
    }

    <T, U, R> BiFunction<T, U, R> contextualFunctionUnlessContextualized(BiFunction<T, U, R> function) {
        if (function instanceof Contextualized)
            return function;
        return contextualFunction(function);
    }

    <T, U, R> BiFunction<T, U, R> contextualFunction(CapturedContextState state, BiFunction<T, U, R> function) {
        checkPrecontextualized(function);
        return new ContextualBiFunction<T, U, R>(state, function);
    }

    @Override
    public <R> Callable<R> contextualCallable(Callable<R> callable) {
        return contextualCallable(manager.captureContext(plan), callable);
    }

    <R> Callable<R> contextualCallableUnlessContextualized(Callable<R> callable) {
        if (callable instanceof Contextualized)
            return callable;
        return contextualCallable(callable);
    }

    <R> Callable<R> contextualCallable(CapturedContextState state, Callable<R> callable) {
        checkPrecontextualized(callable);
        return new ContextualCallable<R>(state, callable);
    }

    @Override
    public <T> Consumer<T> contextualConsumer(Consumer<T> consumer) {
        return contextualConsumer(manager.captureContext(plan), consumer);
    }

    <T> Consumer<T> contextualConsumerUnlessContextualized(Consumer<T> consumer) {
        if (consumer instanceof Contextualized)
            return consumer;
        return contextualConsumer(consumer);
    }

    <T> Consumer<T> contextualConsumer(CapturedContextState state, Consumer<T> consumer) {
        checkPrecontextualized(consumer);
        return new ContextualConsumer<T>(state, consumer);
    }

    @Override
    public <T, R> Function<T, R> contextualFunction(Function<T, R> function) {
        return contextualFunction(manager.captureContext(plan), function);
    }

    <T, R> Function<T, R> contextualFunctionUnlessContextualized(Function<T, R> function) {
        if (function instanceof Contextualized)
            return function;
        return contextualFunction(function);
    }

    <T, R> Function<T, R> contextualFunction(CapturedContextState state, Function<T, R> function) {
        checkPrecontextualized(function);
        return new ContextualFunction<T, R>(state, function);
    }

    @Override
    public Runnable contextualRunnable(Runnable runnable) {
        return contextualRunnable(manager.captureContext(plan), runnable);
    }

    Runnable contextualRunnableUnlessContextualized(Runnable runnable) {
        if (runnable instanceof Contextualized)
            return runnable;
        return contextualRunnable(runnable);
    }

    Runnable contextualRunnable(CapturedContextState state, Runnable runnable) {
        checkPrecontextualized(runnable);
        return new ContextualRunnable(runnable, state);
    }

    @Override
    public <R> Supplier<R> contextualSupplier(Supplier<R> supplier) {
        return contextualSupplier(manager.captureContext(plan), supplier);
    }

    <R> Supplier<R> contextualSupplierUnlessContextualized(Supplier<R> supplier) {
        if (supplier instanceof Contextualized)
            return supplier;
        return contextualSupplier(supplier);
    }

    <R> Supplier<R> contextualSupplier(CapturedContextState state, Supplier<R> supplier) {
        checkPrecontextualized(supplier);
        return new ContextualSupplier<R>(state, supplier);
    }

    @Override
    public String toString() {
        final String DELIMITER = ", ";
        StringBuilder builder = new StringBuilder();
        builder.append(SmallRyeThreadContext.class.getName()).append(DELIMITER);
        builder.append("with cleared contexts: ").append(plan.clearedProviders).append(DELIMITER);
        builder.append("with propagated contexts: ").append(plan.propagatedProviders).append(DELIMITER);
        builder.append("with unchanged contexts: ").append(plan.unchangedProviders);
        if (injectionPointName != null) {
            builder.append(DELIMITER).append(" with injection point name: ").append(injectionPointName);
        }
        return builder.toString();
    }

    public static class Builder implements ThreadContext.Builder {

        private String[] propagated;
        private String[] unchanged;
        private String[] cleared;
        private SmallRyeContextManager manager;
        private String injectionPointName = null;

        public Builder(SmallRyeContextManager manager) {
            this.manager = manager;
            DefaultValues defaultValues = manager.getDefaultValues();
            this.propagated = defaultValues.getThreadPropagated();
            this.unchanged = defaultValues.getThreadUnchanged();
            this.cleared = defaultValues.getThreadCleared();
        }

        @Override
        public SmallRyeThreadContext build() {
            return new SmallRyeThreadContext(manager, propagated, unchanged, cleared, injectionPointName,
                    manager.getDefaultExecutorService());
        }

        @Override
        public Builder propagated(String... types) {
            propagated = types;
            return this;
        }

        @Override
        public Builder unchanged(String... types) {
            unchanged = types;
            return this;
        }

        @Override
        public Builder cleared(String... types) {
            cleared = types;
            return this;
        }

        //
        // Extras

        public Builder injectionPointName(String name) {
            this.injectionPointName = name;
            return this;
        }

    }
}
