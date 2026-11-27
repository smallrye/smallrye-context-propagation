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

import io.smallrye.context.impl.CapturedContextState;
import io.smallrye.context.impl.Contextualized;
import io.smallrye.context.impl.DefaultValues;
import io.smallrye.context.impl.SlowCapturedContextState;
import io.smallrye.context.impl.SmallRyeThreadContextStorageDeclaration;
import io.smallrye.context.impl.ThreadContextProviderPlan;
import io.smallrye.context.impl.wrappers.ContextualBiConsumer;
import io.smallrye.context.impl.wrappers.ContextualBiConsumer1;
import io.smallrye.context.impl.wrappers.ContextualBiConsumer2;
import io.smallrye.context.impl.wrappers.ContextualBiConsumerN;
import io.smallrye.context.impl.wrappers.ContextualBiFunction;
import io.smallrye.context.impl.wrappers.ContextualBiFunction1;
import io.smallrye.context.impl.wrappers.ContextualBiFunction2;
import io.smallrye.context.impl.wrappers.ContextualBiFunctionN;
import io.smallrye.context.impl.wrappers.ContextualCallable;
import io.smallrye.context.impl.wrappers.ContextualCallable1;
import io.smallrye.context.impl.wrappers.ContextualCallable2;
import io.smallrye.context.impl.wrappers.ContextualCallableN;
import io.smallrye.context.impl.wrappers.ContextualConsumer;
import io.smallrye.context.impl.wrappers.ContextualConsumer1;
import io.smallrye.context.impl.wrappers.ContextualConsumer2;
import io.smallrye.context.impl.wrappers.ContextualConsumerN;
import io.smallrye.context.impl.wrappers.ContextualExecutor;
import io.smallrye.context.impl.wrappers.ContextualExecutor1;
import io.smallrye.context.impl.wrappers.ContextualExecutor2;
import io.smallrye.context.impl.wrappers.ContextualExecutorN;
import io.smallrye.context.impl.wrappers.ContextualFunction;
import io.smallrye.context.impl.wrappers.ContextualFunction1;
import io.smallrye.context.impl.wrappers.ContextualFunction2;
import io.smallrye.context.impl.wrappers.ContextualFunctionN;
import io.smallrye.context.impl.wrappers.ContextualRunnable;
import io.smallrye.context.impl.wrappers.ContextualRunnable1;
import io.smallrye.context.impl.wrappers.ContextualRunnable2;
import io.smallrye.context.impl.wrappers.ContextualRunnableN;
import io.smallrye.context.impl.wrappers.ContextualSupplier;
import io.smallrye.context.impl.wrappers.ContextualSupplier1;
import io.smallrye.context.impl.wrappers.ContextualSupplier2;
import io.smallrye.context.impl.wrappers.ContextualSupplierN;
import io.smallrye.context.impl.wrappers.SlowContextualBiConsumer;
import io.smallrye.context.impl.wrappers.SlowContextualBiFunction;
import io.smallrye.context.impl.wrappers.SlowContextualCallable;
import io.smallrye.context.impl.wrappers.SlowContextualConsumer;
import io.smallrye.context.impl.wrappers.SlowContextualExecutor;
import io.smallrye.context.impl.wrappers.SlowContextualFunction;
import io.smallrye.context.impl.wrappers.SlowContextualRunnable;
import io.smallrye.context.impl.wrappers.SlowContextualSupplier;
import io.smallrye.context.storage.spi.StorageManager;

public class SmallRyeThreadContext implements ThreadContext {

    private static final String CLOSE_SETTING_NULL_PROP_NAME = "io.smallrye.context.storage.CLOSE_SETTING_NULL";
    private static final boolean CLOSE_SETTING_NULL = Boolean.getBoolean(CLOSE_SETTING_NULL_PROP_NAME);
    final static ThreadLocal<SmallRyeThreadContext> currentThreadContext = StorageManager
            .threadLocal(SmallRyeThreadContextStorageDeclaration.class);

    private final static CleanAutoCloseable NULL_THREAD_STATE = new CleanAutoCloseable() {
        @Override
        public void close() {
            if (!CLOSE_SETTING_NULL) {
                currentThreadContext.remove();
            } else {
                currentThreadContext.set(null);
            }
        }
    };

    private static final Executor PASSTHROUGH_EXECUTOR = new Executor() {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    };

    /**
     * Updates the current @{link SmallRyeThreadContext} in use by the current thread, and returns an
     * object suitable for use in try-with-resource to restore the previous value.
     *
     * @param threadContext the @{link SmallRyeThreadContext} to use
     * @return an object suitable for use in try-with-resource to restore the previous value.
     */
    public static CleanAutoCloseable withThreadContext(SmallRyeThreadContext threadContext) {
        SmallRyeThreadContext oldValue = currentThreadContext.get();
        currentThreadContext.set(threadContext);
        if (oldValue == null) {
            //For restoring null values we can optimise this a little:
            return NULL_THREAD_STATE;
        } else {
            return new CleanAutoCloseable() {
                @Override
                public void close() {
                    currentThreadContext.set(oldValue);
                }
            };
        }

    }

    /**
     * Invokes the given @{link Runnable} with the current @{link SmallRyeThreadContext} updated to the given value
     * for the current thread.
     *
     * @param threadContext the @{link SmallRyeThreadContext} to use
     * @param f the @{link Runnable} to invoke
     */
    public static void withThreadContext(SmallRyeThreadContext threadContext, Runnable f) {
        final SmallRyeThreadContext oldValue = currentThreadContext.get();
        currentThreadContext.set(threadContext);
        try {
            f.run();
        } finally {
            if (!CLOSE_SETTING_NULL && oldValue == null)
                currentThreadContext.remove();
            } else {
                currentThreadContext.set(oldValue);
            }
        }
    }

    /**
     * Invokes the given @{link Supplier} with the current @{link SmallRyeThreadContext} updated to the given value
     * for the current thread.
     *
     * @param threadContext the @{link SmallRyeThreadContext} to use
     * @param f the @{link Supplier} to invoke
     * @param <T> The type of @{link Supplier} to return
     * @return The value returned by the @{link Supplier}
     */
    public static <T> T withThreadContext(SmallRyeThreadContext threadContext, Supplier<T> f) {
        final SmallRyeThreadContext oldValue = currentThreadContext.get();
        currentThreadContext.set(threadContext);
        try {
            return f.get();
        } finally {
            if (!CLOSE_SETTING_NULL && oldValue == null) {
                currentThreadContext.remove();
            } else {
                currentThreadContext.set(oldValue);
            }
        }
    }

    /**
     * Returns the current thread's @{link SmallRyeThreadContext} if set, or a @{link SmallRyeThreadContext}
     * with default contexts, possibly configured via MP Config.
     *
     * @return the current thread's @{link SmallRyeThreadContext} if set, or a @{link SmallRyeThreadContext}
     *         with default contexts, possibly configured via MP Config.
     */
    public static SmallRyeThreadContext getCurrentThreadContextOrDefaultContexts() {
        return getCurrentThreadContext(SmallRyeContextManagerProvider.getManager().defaultThreadContext());
    }

    /**
     * Returns the current thread's @{link SmallRyeThreadContext} if set, or a @{link SmallRyeThreadContext}
     * which propagates all contexts.
     *
     * @return the current thread's @{link SmallRyeThreadContext} if set, or a @{link SmallRyeThreadContext}
     *         which propagates all contexts.
     */
    public static SmallRyeThreadContext getCurrentThreadContextOrPropagatedContexts() {
        return getCurrentThreadContext(SmallRyeContextManagerProvider.getManager().allPropagatedThreadContext());
    }

    /**
     * Returns the current thread's @{link SmallRyeThreadContext} if set, or a @{link SmallRyeThreadContext}
     * which clears all contexts.
     *
     * @return the current thread's @{link SmallRyeThreadContext} if set, or a @{link SmallRyeThreadContext}
     *         which clears all contexts.
     */
    public static SmallRyeThreadContext getCurrentThreadContextOrClearedContexts() {
        return getCurrentThreadContext(SmallRyeContextManagerProvider.getManager().allClearedThreadContext());
    }

    /**
     * Returns the current thread's @{link SmallRyeThreadContext} if set, or the given @{link SmallRyeThreadContext}
     * default value.
     *
     * @param defaultValue the default value to use
     * @return the current thread's @{link SmallRyeThreadContext} if set, or the given @{link SmallRyeThreadContext}
     *         default value.
     */
    public static SmallRyeThreadContext getCurrentThreadContext(SmallRyeThreadContext defaultValue) {
        SmallRyeThreadContext threadContext = currentThreadContext.get();
        return threadContext != null ? threadContext : defaultValue;
    }

    /**
     * Returns the current thread's @{link SmallRyeThreadContext} if set, or null.
     *
     * @return the current thread's @{link SmallRyeThreadContext} if set, or null.
     */
    public static SmallRyeThreadContext getCurrentThreadContext() {
        return getCurrentThreadContext(null);
    }

    private final ThreadContextProviderPlan plan;
    private final String injectionPointName;
    private final ExecutorService defaultExecutor;

    public SmallRyeThreadContext(SmallRyeContextManager manager, String[] propagated, String[] unchanged,
            String[] cleared, String injectionPointName, ExecutorService defaultExecutor) {
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

    /**
     * Returns true if this thread context has no context to propagate nor clear, and so
     * will not contextualise anything.
     *
     * @return true if this thread context has no context to propagate nor clear
     */
    public boolean isEmpty() {
        return plan.isEmpty();
    }

    /**
     * Returns true if the given lambda instance is already contextualized
     *
     * @param lambda the lambda to test
     * @return true if the given lambda instance is already contextualized
     */
    public boolean isContextualized(Object lambda) {
        return lambda instanceof Contextualized;
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
        CompletableFuture<T> ret = JdkSpecific.newCompletableFuture(this, executor);
        future.whenComplete((val, x) -> {
            if (x != null) {
                ret.completeExceptionally(x);
            } else {
                ret.complete(val);
            }
        });
        return ret;
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
        // the MP-CP TCK insists we cannot complete instances returned by this API
        ContextualCompletableFuture<T> ret = (ContextualCompletableFuture<T>) JdkSpecific.newCompletionStage(this, executor);
        stage.whenComplete((val, x) -> {
            if (x != null) {
                ret.superCompleteExceptionally(x);
            } else {
                ret.superComplete(val);
            }
        });
        return ret;
    }

    /**
     * <p>
     * Returns a new CompletableFuture that is already completed with the specified value.
     * </p>
     *
     * <p>
     * The new completable future will use the same default executor as this ThreadContext,
     * which can be a ManagedExecutor if this ThreadContext was obtained by {@link SmallRyeManagedExecutor#getThreadContext()}
     * or the default executor service as set by
     * {@link SmallRyeContextManager.Builder#withDefaultExecutorService(ExecutorService)},
     * or otherwise have no default executor.
     * </p>
     *
     * @param value result with which the completable future is completed.
     * @param <U> result type of the completable future.
     * @return the new completable future.
     */
    public <U> CompletableFuture<U> completedFuture(U value) {
        CompletableFuture<U> ret = newIncompleteFuture();
        ret.complete(value);
        return ret;
    }

    /**
     * <p>
     * Returns a new CompletionStage that is already completed with the specified value.
     * </p>
     *
     * <p>
     * The new completion stage will use the same default executor as this ThreadContext,
     * which can be a ManagedExecutor if this ThreadContext was obtained by {@link SmallRyeManagedExecutor#getThreadContext()}
     * or the default executor service as set by
     * {@link SmallRyeContextManager.Builder#withDefaultExecutorService(ExecutorService)},
     * or otherwise have no default executor.
     * </p>
     *
     * @param value result with which the completable future is completed.
     * @param <U> result type of the completion stage.
     * @return the new completion stage.
     */
    public <U> CompletionStage<U> completedStage(U value) {
        return completedFuture(value);
    }

    /**
     * <p>
     * Returns a new CompletableFuture that is already exceptionally completed with the specified Throwable.
     * </p>
     *
     * <p>
     * The new completable future will use the same default executor as this ThreadContext,
     * which can be a ManagedExecutor if this ThreadContext was obtained by {@link SmallRyeManagedExecutor#getThreadContext()}
     * or the default executor service as set by
     * {@link SmallRyeContextManager.Builder#withDefaultExecutorService(ExecutorService)},
     * or otherwise have no default executor.
     * </p>
     *
     * @param ex exception or error with which the completable future is completed.
     * @param <U> result type of the completable future.
     * @return the new completable future.
     */
    public <U> CompletableFuture<U> failedFuture(Throwable ex) {
        CompletableFuture<U> ret = newIncompleteFuture();
        ret.completeExceptionally(ex);
        return ret;
    }

    /**
     * <p>
     * Returns a new CompletionStage that is already exceptionally completed with the specified Throwable.
     * </p>
     *
     * <p>
     * The new completion stage will use the same default executor as this ThreadContext,
     * which can be a ManagedExecutor if this ThreadContext was obtained by {@link SmallRyeManagedExecutor#getThreadContext()}
     * or the default executor service as set by
     * {@link SmallRyeContextManager.Builder#withDefaultExecutorService(ExecutorService)},
     * or otherwise have no default executor.
     * </p>
     *
     * @param ex exception or error with which the stage is completed.
     * @param <U> result type of the completion stage.
     * @return the new completion stage.
     */
    public <U> CompletionStage<U> failedStage(Throwable ex) {
        return failedFuture(ex);
    }

    /**
     * <p>
     * Returns a new incomplete <code>CompletableFuture</code>.
     * </p>
     *
     * <p>
     * The new completable future will use the same default executor as this ThreadContext,
     * which can be a ManagedExecutor if this ThreadContext was obtained by {@link SmallRyeManagedExecutor#getThreadContext()}
     * or the default executor service as set by
     * {@link SmallRyeContextManager.Builder#withDefaultExecutorService(ExecutorService)},
     * or otherwise have no default executor.
     * </p>
     *
     * @param <U> result type of the completion stage.
     * @return the new completion stage.
     */
    public <U> CompletableFuture<U> newIncompleteFuture() {
        return JdkSpecific.newCompletableFuture(this, defaultExecutor);
    }

    /**
     * <p>
     * Returns a new <code>CompletableFuture</code> that is completed by the completion of the
     * specified stage.
     * </p>
     *
     * <p>
     * The new completable future is backed by the ManagedExecutor upon which copy is invoked,
     * which serves as the default asynchronous execution facility
     * for the new stage and all dependent stages created from it, and so forth.
     * </p>
     *
     * <p>
     * When dependent stages are created from the new completable future, thread context is captured
     * and/or cleared by the ManagedExecutor. This guarantees that the action
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
     * @param stage a completable future whose completion triggers completion of the new completable
     *        future that is created by this method.
     * @return the new completable future.
     */
    public <T> CompletableFuture<T> copy(CompletableFuture<T> stage) {
        return withContextCapture(stage);
    }

    /**
     * <p>
     * Returns a new <code>CompletionStage</code> that is completed by the completion of the
     * specified stage.
     * </p>
     *
     * <p>
     * The new completable future is backed by the ManagedExecutor upon which copy is invoked,
     * which serves as the default asynchronous execution facility
     * for the new stage and all dependent stages created from it, and so forth.
     * </p>
     *
     * <p>
     * When dependent stages are created from the new completable future, thread context is captured
     * and/or cleared by the ManagedExecutor. This guarantees that the action
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
    public <T> CompletionStage<T> copy(CompletionStage<T> stage) {
        return withContextCapture(stage);
    }

    @Override
    public Executor currentContextExecutor() {
        if (plan.isEmpty())
            return PASSTHROUGH_EXECUTOR;
        if (plan.isFast()) {
            ContextualExecutor ret = null;
            switch (plan.size()) {
                case 1:
                    ret = new ContextualExecutor1();
                    break;
                case 2:
                    ret = new ContextualExecutor2();
                    break;
                default:
                    ret = new ContextualExecutorN(plan.size());
                    break;
            }
            plan.takeThreadContextSnapshotsFast(this, currentThreadContext, ret);
            return ret;
        }
        return new SlowContextualExecutor(captureContext());
    }

    @Override
    public <T, U> BiConsumer<T, U> contextualConsumer(BiConsumer<T, U> consumer) {
        checkPrecontextualized(consumer);
        if (plan.isEmpty())
            return consumer;
        if (plan.isFast()) {
            ContextualBiConsumer<T, U> ret = null;
            switch (plan.size()) {
                case 1:
                    ret = new ContextualBiConsumer1<>(consumer);
                    break;
                case 2:
                    ret = new ContextualBiConsumer2<>(consumer);
                    break;
                default:
                    ret = new ContextualBiConsumerN<>(consumer, plan.size());
                    break;
            }
            plan.takeThreadContextSnapshotsFast(this, currentThreadContext, ret);
            return ret;
        }
        return new SlowContextualBiConsumer<>(captureContext(), consumer);
    }

    <T, U> BiConsumer<T, U> contextualConsumerUnlessContextualized(BiConsumer<T, U> consumer) {
        if (consumer instanceof Contextualized)
            return consumer;
        return contextualConsumer(consumer);
    }

    @Override
    public <T, U, R> BiFunction<T, U, R> contextualFunction(BiFunction<T, U, R> function) {
        checkPrecontextualized(function);
        if (plan.isEmpty())
            return function;
        if (plan.isFast()) {
            ContextualBiFunction<T, U, R> ret = null;
            switch (plan.size()) {
                case 1:
                    ret = new ContextualBiFunction1<>(function);
                    break;
                case 2:
                    ret = new ContextualBiFunction2<>(function);
                    break;
                default:
                    ret = new ContextualBiFunctionN<>(function, plan.size());
                    break;
            }
            plan.takeThreadContextSnapshotsFast(this, currentThreadContext, ret);
            return ret;
        }
        return new SlowContextualBiFunction<>(captureContext(), function);
    }

    <T, U, R> BiFunction<T, U, R> contextualFunctionUnlessContextualized(BiFunction<T, U, R> function) {
        if (function instanceof Contextualized)
            return function;
        return contextualFunction(function);
    }

    @Override
    public <R> Callable<R> contextualCallable(Callable<R> callable) {
        checkPrecontextualized(callable);
        if (plan.isEmpty())
            return callable;
        if (plan.isFast()) {
            ContextualCallable<R> ret = null;
            switch (plan.size()) {
                case 1:
                    ret = new ContextualCallable1<>(callable);
                    break;
                case 2:
                    ret = new ContextualCallable2<>(callable);
                    break;
                default:
                    ret = new ContextualCallableN<>(callable, plan.size());
                    break;
            }
            plan.takeThreadContextSnapshotsFast(this, currentThreadContext, ret);
            return ret;
        }
        return new SlowContextualCallable<>(captureContext(), callable);
    }

    <R> Callable<R> contextualCallableUnlessContextualized(Callable<R> callable) {
        if (callable instanceof Contextualized)
            return callable;
        return contextualCallable(callable);
    }

    @Override
    public <T> Consumer<T> contextualConsumer(Consumer<T> consumer) {
        checkPrecontextualized(consumer);
        if (plan.isEmpty())
            return consumer;
        if (plan.isFast()) {
            ContextualConsumer<T> ret = null;
            switch (plan.size()) {
                case 1:
                    ret = new ContextualConsumer1<>(consumer);
                    break;
                case 2:
                    ret = new ContextualConsumer2<>(consumer);
                    break;
                default:
                    ret = new ContextualConsumerN<>(consumer, plan.size());
                    break;
            }
            plan.takeThreadContextSnapshotsFast(this, currentThreadContext, ret);
            return ret;
        }
        return new SlowContextualConsumer<>(captureContext(), consumer);
    }

    <T> Consumer<T> contextualConsumerUnlessContextualized(Consumer<T> consumer) {
        if (consumer instanceof Contextualized)
            return consumer;
        return contextualConsumer(consumer);
    }

    @Override
    public <T, R> Function<T, R> contextualFunction(Function<T, R> function) {
        checkPrecontextualized(function);
        if (plan.isEmpty())
            return function;
        if (plan.isFast()) {
            ContextualFunction<T, R> ret = null;
            switch (plan.size()) {
                case 1:
                    ret = new ContextualFunction1<>(function);
                    break;
                case 2:
                    ret = new ContextualFunction2<>(function);
                    break;
                default:
                    ret = new ContextualFunctionN<>(function, plan.size());
                    break;
            }
            plan.takeThreadContextSnapshotsFast(this, currentThreadContext, ret);
            return ret;
        }
        return new SlowContextualFunction<>(captureContext(), function);
    }

    <T, R> Function<T, R> contextualFunctionUnlessContextualized(Function<T, R> function) {
        if (function instanceof Contextualized)
            return function;
        return contextualFunction(function);
    }

    @Override
    public Runnable contextualRunnable(Runnable runnable) {
        checkPrecontextualized(runnable);
        if (plan.isEmpty())
            return runnable;
        if (plan.isFast()) {
            ContextualRunnable ret = null;
            switch (plan.size()) {
                case 1:
                    ret = new ContextualRunnable1(runnable);
                    break;
                case 2:
                    ret = new ContextualRunnable2(runnable);
                    break;
                default:
                    ret = new ContextualRunnableN(runnable, plan.size());
                    break;
            }
            plan.takeThreadContextSnapshotsFast(this, currentThreadContext, ret);
            return ret;
        }
        return new SlowContextualRunnable(captureContext(), runnable);
    }

    Runnable contextualRunnableUnlessContextualized(Runnable runnable) {
        if (runnable instanceof Contextualized)
            return runnable;
        return contextualRunnable(runnable);
    }

    @Override
    public <R> Supplier<R> contextualSupplier(Supplier<R> supplier) {
        checkPrecontextualized(supplier);
        if (plan.isEmpty())
            return supplier;
        if (plan.isFast()) {
            ContextualSupplier<R> ret = null;
            switch (plan.size()) {
                case 1:
                    ret = new ContextualSupplier1<>(supplier);
                    break;
                case 2:
                    ret = new ContextualSupplier2<>(supplier);
                    break;
                default:
                    ret = new ContextualSupplierN<>(supplier, plan.size());
                    break;
            }
            plan.takeThreadContextSnapshotsFast(this, currentThreadContext, ret);
            return ret;
        }
        return new SlowContextualSupplier<>(captureContext(), supplier);
    }

    <R> Supplier<R> contextualSupplierUnlessContextualized(Supplier<R> supplier) {
        if (supplier instanceof Contextualized)
            return supplier;
        return contextualSupplier(supplier);
    }

    private CapturedContextState captureContext() {
        return new SlowCapturedContextState(this);
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
        private final SmallRyeContextManager manager;
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
