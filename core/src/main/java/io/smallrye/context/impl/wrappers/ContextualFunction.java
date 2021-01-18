package io.smallrye.context.impl.wrappers;

import java.util.function.Function;

import io.smallrye.context.impl.ContextHolder;
import io.smallrye.context.impl.Contextualized;

public interface ContextualFunction<T, R> extends Function<T, R>, Contextualized, ContextHolder {

}
