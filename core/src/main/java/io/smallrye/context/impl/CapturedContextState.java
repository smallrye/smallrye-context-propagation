package io.smallrye.context.impl;

import java.util.concurrent.Callable;

import io.smallrye.context.CleanAutoCloseable;

@FunctionalInterface
public interface CapturedContextState {
    <T> CleanAutoCloseable<T> begin(Callable<T> callable);
}
