package io.smallrye.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.threads.ViewExecutor;

import io.smallrye.context.impl.DefaultValues;
import io.smallrye.context.impl.ThreadContextProviderPlan;

public class SmallRyeManagedExecutor implements ManagedExecutor {

    private final SmallRyeThreadContext threadContext;
    private final int maxAsync;
    private final int maxQueued;
    private final String injectionPointName;
    private final ExecutorService executor;

    private class NoPropagationExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            SmallRyeManagedExecutor.this.executeWithoutPropagation(command);
        }
    }

    private final Executor noPropagationExecutor = new NoPropagationExecutor();

    public static ExecutorService newThreadPoolExecutor(int maxAsync, int maxQueued) {
        ThreadPoolExecutor exec = new ThreadPoolExecutor(maxAsync == -1 ? Runtime.getRuntime().availableProcessors() : maxAsync,
                maxAsync == -1 ? Runtime.getRuntime().availableProcessors() : maxAsync, 5000l, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(maxQueued == -1 ? Integer.MAX_VALUE : maxQueued),
                new ThreadPoolExecutor.AbortPolicy());
        // we set core thread == max threads but allow for core thread timeout
        // this prevents delaying spawning of new thread to when the queue is full
        exec.allowCoreThreadTimeOut(true);
        return exec;
    }

    public SmallRyeManagedExecutor(int maxAsync, int maxQueued, SmallRyeThreadContext threadContext, ExecutorService executor,
            String injectionPointName) {
        this.threadContext = threadContext;
        this.maxAsync = maxAsync;
        this.maxQueued = maxQueued;
        this.injectionPointName = injectionPointName;
        this.executor = executor;
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executor.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(threadContext.contextualCallableUnlessContextualized(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return executor.submit(threadContext.contextualRunnableUnlessContextualized(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return executor.submit(threadContext.contextualRunnableUnlessContextualized(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        List<Callable<T>> wrappedTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            wrappedTasks.add(threadContext.contextualCallableUnlessContextualized(task));
        }
        return executor.invokeAll(wrappedTasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        List<Callable<T>> wrappedTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            wrappedTasks.add(threadContext.contextualCallableUnlessContextualized(task));
        }
        return executor.invokeAll(wrappedTasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        List<Callable<T>> wrappedTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            wrappedTasks.add(threadContext.contextualCallableUnlessContextualized(task));
        }
        return executor.invokeAny(wrappedTasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        List<Callable<T>> wrappedTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            wrappedTasks.add(threadContext.contextualCallableUnlessContextualized(task));
        }
        return executor.invokeAny(wrappedTasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        executor.execute(threadContext.contextualRunnableUnlessContextualized(command));
    }

    private void executeWithoutPropagation(Runnable command) {
        executor.execute(command);
    }

    @Override
    public <U> CompletableFuture<U> completedFuture(U value) {
        return threadContext.withContextCapture(CompletableFuture.completedFuture(value), this);
    }

    @Override
    public <U> CompletionStage<U> completedStage(U value) {
        return completedFuture(value);
    }

    @Override
    public <U> CompletableFuture<U> failedFuture(Throwable ex) {
        CompletableFuture<U> ret = new CompletableFuture<>();
        ret.completeExceptionally(ex);
        return threadContext.withContextCapture(ret, this);
    }

    @Override
    public <U> CompletionStage<U> failedStage(Throwable ex) {
        return failedFuture(ex);
    }

    @Override
    public CompletableFuture<Void> runAsync(Runnable runnable) {
        // runAsync wraps the contextual function we give it in its own function and calls execute() on the executor
        // we pass it, so to avoid double contextualisation we pass it a non-propagating executor
        // if we contextualise the function it passes to execute(), then our begin/endContext calls will run outside
        // of any thread synchronisation such as join() and it would be all sorts of wrong
        return threadContext.withContextCapture(CompletableFuture
                .runAsync(threadContext.contextualRunnableUnlessContextualized(runnable), noPropagationExecutor), this);
    }

    @Override
    public <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        // runAsync wraps the contextual function we give it in its own function and calls execute() on the executor
        // we pass it, so to avoid double contextualisation we pass it a non-propagating executor
        // if we contextualise the function it passes to execute(), then our begin/endContext calls will run outside
        // of any thread synchronisation such as join() and it would be all sorts of wrong
        return threadContext.withContextCapture(CompletableFuture
                .supplyAsync(threadContext.contextualSupplierUnlessContextualized(supplier), noPropagationExecutor), this);
    }

    @Override
    public <U> CompletableFuture<U> newIncompleteFuture() {
        CompletableFuture<U> ret = new CompletableFuture<>();
        return threadContext.withContextCapture(ret, this);
    }

    @Override
    public String toString() {
        final String DELIMITER = ", ";
        StringBuilder builder = new StringBuilder();
        builder.append(SmallRyeManagedExecutor.class.getName()).append(DELIMITER);
        builder.append("with maxAsync: ").append(maxAsync).append(DELIMITER);
        builder.append("with maxQueued: ").append(maxQueued).append(DELIMITER);
        builder.append("with cleared contexts: ").append(threadContext.getPlan().clearedProviders).append(DELIMITER);
        builder.append("with propagated contexts: ").append(threadContext.getPlan().propagatedProviders).append(DELIMITER);
        builder.append("with unchanged contexts: ").append(threadContext.getPlan().unchangedProviders);
        if (injectionPointName != null) {
            builder.append(DELIMITER).append(" with injection point name: ").append(injectionPointName);
        }
        return builder.toString();
    }

    public ThreadContextProviderPlan getThreadContextProviderPlan() {
        return threadContext.getPlan();
    }

    public int getMaxAsync() {
        return maxAsync;
    }

    public int getMaxQueued() {
        return maxQueued;
    }

    public String getInjectionPointName() {
        return injectionPointName;
    }

    public static class Builder implements ManagedExecutor.Builder {

        private SmallRyeContextManager manager;
        private int maxAsync;
        private int maxQueued;
        private String[] propagated;
        private String[] cleared;
        private String injectionPointName = null;
        private ExecutorService executorService;

        public Builder(SmallRyeContextManager manager) {
            this.manager = manager;
            DefaultValues defaultValues = manager.getDefaultValues();
            // initiate with default values
            this.propagated = defaultValues.getExecutorPropagated();
            this.cleared = defaultValues.getExecutorCleared();
            this.maxAsync = defaultValues.getExecutorAsync();
            this.maxQueued = defaultValues.getExecutorQueue();
        }

        @Override
        public SmallRyeManagedExecutor build() {
            ExecutorService executor;
            if (executorService != null)
                executor = ViewExecutor.builder(executorService).setMaxSize(maxAsync).setQueueLimit(maxQueued).build();
            else
                executor = SmallRyeManagedExecutor.newThreadPoolExecutor(maxAsync, maxQueued);
            return new SmallRyeManagedExecutor(maxAsync, maxQueued,
                    new SmallRyeThreadContext(manager, propagated, SmallRyeContextManager.NO_STRING, cleared),
                    executor, injectionPointName);
        }

        @Override
        public Builder propagated(String... types) {
            this.propagated = types;
            return this;
        }

        @Override
        public Builder maxAsync(int max) {
            if (max == 0 || max < -1) {
                throw new IllegalArgumentException("ManagedExecutor parameter maxAsync cannot be 0 or lower then -1.");
            }
            this.maxAsync = max;
            return this;
        }

        @Override
        public Builder maxQueued(int max) {
            if (max == 0 || max < -1) {
                throw new IllegalArgumentException("ManagedExecutor parameter maxQueued cannot be 0 or lower than -1.");
            }
            this.maxQueued = max;
            return this;
        }

        @Override
        public Builder cleared(String... types) {
            this.cleared = types;
            return this;
        }

        //
        // Extras

        public Builder injectionPointName(String name) {
            this.injectionPointName = name;
            return this;
        }

        public Builder withExecutorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

    }
}
