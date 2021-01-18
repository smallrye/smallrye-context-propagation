package io.smallrye.context.impl.wrappers;

import java.util.function.BiFunction;

import io.smallrye.context.impl.ContextHolder;
import io.smallrye.context.impl.Contextualized;

public interface ContextualBiFunction<T, U, R> extends BiFunction<T, U, R>, Contextualized, ContextHolder {

}
