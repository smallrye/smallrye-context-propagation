package io.smallrye.concurrency.impl;

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

public class ThreadContextImpl implements ThreadContext {

	private SmallRyeConcurrencyManager manager;
	private String[] unchanged;
	private String[] propagated;

	public ThreadContextImpl(SmallRyeConcurrencyManager manager, String[] propagated, String[] unchanged) {
		this.manager = manager;
		this.propagated = propagated;
		this.unchanged = unchanged;
	}

	//
	// Wrappers
	
	// FIXME: move in ThreadContext https://github.com/eclipse/microprofile-concurrency/issues/9
	public <T> CompletableFuture<T> withCurrentContext(CompletableFuture<T> future){
		return withCurrentContext(future, null);
	}

	<T> CompletableFuture<T> withCurrentContext(CompletableFuture<T> future, ManagedExecutor executor){
		return withContext(manager.captureContext(propagated, unchanged), future, executor);
	}

	<T> CompletableFuture<T> withContext(CapturedContextState state, CompletableFuture<T> future, ManagedExecutor executor){
		return new CompletableFutureWrapper<>(this, state, future, executor);
	}

	// FIXME: move in ThreadContext https://github.com/eclipse/microprofile-concurrency/issues/9
	public <T> CompletionStage<T> withCurrentContext(CompletionStage<T> future){
		return withContext(manager.captureContext(propagated, unchanged), future);
	}

	<T> CompletionStage<T> withContext(CapturedContextState state, CompletionStage<T> future){
		return new CompletionStageWrapper<>(this, state, future);
	}

	@Override
	public Executor withCurrentContext() {
		return withContext(manager.captureContext(propagated, unchanged));
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
	public <T, U> BiConsumer<T, U> withCurrentContext(BiConsumer<T, U> consumer) {
		return withContext(manager.captureContext(propagated, unchanged), consumer);
	}

	<T, U> BiConsumer<T, U> withContext(CapturedContextState state, BiConsumer<T, U> consumer) {
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
	public <T, U, R> BiFunction<T, U, R> withCurrentContext(BiFunction<T, U, R> function) {
		return withContext(manager.captureContext(propagated, unchanged), function);
	}

	<T, U, R> BiFunction<T, U, R> withContext(CapturedContextState state, BiFunction<T, U, R> function) {
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
	public <R> Callable<R> withCurrentContext(Callable<R> callable) {
		return withContext(manager.captureContext(propagated, unchanged), callable);
	}

	<R> Callable<R> withContext(CapturedContextState state, Callable<R> callable) {
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
	public <T> Consumer<T> withCurrentContext(Consumer<T> consumer) {
		return withContext(manager.captureContext(propagated, unchanged), consumer);
	}
	
	<T> Consumer<T> withContext(CapturedContextState state, Consumer<T> consumer) {
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
	public <T, R> Function<T, R> withCurrentContext(Function<T, R> function) {
		return withContext(manager.captureContext(propagated, unchanged), function);
	}

	<T, R> Function<T, R> withContext(CapturedContextState state, Function<T, R> function) {
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
	public Runnable withCurrentContext(Runnable runnable) {
		return withContext(manager.captureContext(propagated, unchanged), runnable);
	}
	
	Runnable withContext(CapturedContextState state, Runnable runnable) {
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
	public <R> Supplier<R> withCurrentContext(Supplier<R> supplier) {
		return withContext(manager.captureContext(propagated, unchanged), supplier);
	}

	<R> Supplier<R> withContext(CapturedContextState state, Supplier<R> supplier) {
		return () -> {
			ActiveContextState activeState = state.begin();
			try {
				return supplier.get();
			}finally {
				activeState.endContext();
			}
		};
	}

}
