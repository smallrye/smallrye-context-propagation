package io.smallrye.context.impl.wrappers;

import java.util.function.BiConsumer;

import io.smallrye.context.impl.ContextHolder;
import io.smallrye.context.impl.Contextualized;

public interface ContextualBiConsumer<T, U> extends BiConsumer<T, U>, Contextualized, ContextHolder {

}
