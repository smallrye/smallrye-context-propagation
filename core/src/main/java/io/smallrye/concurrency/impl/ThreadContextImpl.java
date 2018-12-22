package io.smallrye.concurrency.impl;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.ThreadContext;

import io.smallrye.concurrency.ActiveContextState;
import io.smallrye.concurrency.CapturedContextState;
import io.smallrye.concurrency.SmallRyeConcurrencyManager;
import org.eclipse.microprofile.concurrent.ThreadContextConfig;

public class ThreadContextImpl implements ThreadContext {

	private SmallRyeConcurrencyManager manager;
	private String[] cleared;
	private String[] propagated;
	private String[] unchanged;

	public ThreadContextImpl(SmallRyeConcurrencyManager manager, String[] propagated, String[] unchanged, String[] cleared) {
		this.manager = manager;
		this.propagated = propagated.length > 0 ? propagated : ThreadContextConfig.Literal.DEFAULT_INSTANCE.propagated();
		this.cleared = cleared.length > 0 ? cleared : ThreadContextConfig.Literal.DEFAULT_INSTANCE.cleared();
		this.unchanged = unchanged.length > 0 ? unchanged : ThreadContextConfig.Literal.DEFAULT_INSTANCE.unchanged();
		// verify that all contexts exist
		verifyContextsExist(propagated, manager);
		verifyContextsExist(cleared, manager);
		verifyContextsExist(unchanged, manager);
	}

	// For Tests
	
	public String[] getPropagated() {
		return Arrays.copyOf(propagated, propagated.length);
	}

	public String[] getCleared() {
		return Arrays.copyOf(cleared, cleared.length);
	}

	public String[] getUnchanged() {
		return Arrays.copyOf(unchanged, unchanged.length);
	}

	//
	// Wrappers
	
	@Override
	public <T> CompletableFuture<T> withContextCapture(CompletableFuture<T> future){
		return withContextCapture(future, null);
	}

	<T> CompletableFuture<T> withContextCapture(CompletableFuture<T> future, ManagedExecutor executor){
		return new CompletableFutureWrapper<>(this, future, executor);
	}

	@Override
	public <T> CompletionStage<T> withContextCapture(CompletionStage<T> future){
		return new CompletionStageWrapper<>(this, future);
	}

	@Override
	public Executor currentContextExecutor() {
		return withContext(manager.captureContext(propagated, unchanged, cleared));
	}

	Executor withContext(CapturedContextState state) {
		return (runnable) -> {
			ActiveContextState activeState = state.begin();
			try {
				runnable.run();
			}finally {
				activeState.endContext();
			}
		};
	}

	@Override
	public <T, U> BiConsumer<T, U> contextualConsumer(BiConsumer<T, U> consumer) {
		return contextualConsumer(manager.captureContext(propagated, unchanged, cleared), consumer);
	}

	<T, U> BiConsumer<T, U> contextualConsumer(CapturedContextState state, BiConsumer<T, U> consumer) {
		return (t, u) -> {
			ActiveContextState activeState = state.begin();
			try {
				consumer.accept(t, u);
			}finally {
				activeState.endContext();
			}
		};
	}

	@Override
	public <T, U, R> BiFunction<T, U, R> contextualFunction(BiFunction<T, U, R> function) {
		return contextualFunction(manager.captureContext(propagated, unchanged, cleared), function);
	}

	<T, U, R> BiFunction<T, U, R> contextualFunction(CapturedContextState state, BiFunction<T, U, R> function) {
		return (t, u) -> {
			ActiveContextState activeState = state.begin();
			try {
				return function.apply(t, u);
			}finally {
				activeState.endContext();
			}
		};
	}

	@Override
	public <R> Callable<R> contextualCallable(Callable<R> callable) {
		return contextualCallable(manager.captureContext(propagated, unchanged, cleared), callable);
	}

	<R> Callable<R> contextualCallable(CapturedContextState state, Callable<R> callable) {
		return () -> {
			ActiveContextState activeState = state.begin();
			try {
				return callable.call();
			}finally {
				activeState.endContext();
			}
		};
	}

	@Override
	public <T> Consumer<T> contextualConsumer(Consumer<T> consumer) {
		return contextualConsumer(manager.captureContext(propagated, unchanged, cleared), consumer);
	}
	
	<T> Consumer<T> contextualConsumer(CapturedContextState state, Consumer<T> consumer) {
		return t -> {
			ActiveContextState activeState = state.begin();
			try {
				consumer.accept(t);
			}finally {
				activeState.endContext();
			}
		};
	}

	@Override
	public <T, R> Function<T, R> contextualFunction(Function<T, R> function) {
		return contextualFunction(manager.captureContext(propagated, unchanged, cleared), function);
	}

	<T, R> Function<T, R> contextualFunction(CapturedContextState state, Function<T, R> function) {
		return t -> {
			ActiveContextState activeState = state.begin();
			try {
				return function.apply(t);
			}finally {
				activeState.endContext();
			}
		};
	}

	@Override
	public Runnable contextualRunnable(Runnable runnable) {
		return contextualRunnable(manager.captureContext(propagated, unchanged, cleared), runnable);
	}
	
	Runnable contextualRunnable(CapturedContextState state, Runnable runnable) {
		return () -> {
			ActiveContextState activeState = state.begin();
			try {
				runnable.run();
			}finally {
				activeState.endContext();
			}
		};
	}

	@Override
	public <R> Supplier<R> contextualSupplier(Supplier<R> supplier) {
		return contextualSupplier(manager.captureContext(propagated, unchanged, cleared), supplier);
	}

	<R> Supplier<R> contextualSupplier(CapturedContextState state, Supplier<R> supplier) {
		return () -> {
			ActiveContextState activeState = state.begin();
			try {
				return supplier.get();
			}finally {
				activeState.endContext();
			}
		};
	}

	private void verifyContextsExist(String[] contexts, SmallRyeConcurrencyManager manager) {
		if (contexts.length > 0) {
			Set<String> known = manager.getAllThreadContextProviderNames();
			for (String s : contexts) {
				if (!known.contains(s) && !s.equals(ThreadContext.ALL_REMAINING)) {
					throw new IllegalStateException("Unknown ThreadContext found - " + s);
				}
			}
		}
	}
}
