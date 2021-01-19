package io.smallrye.context.impl.wrappers;

import java.util.function.Consumer;

import io.smallrye.context.impl.ContextHolder;
import io.smallrye.context.impl.Contextualized;

public interface ContextualConsumer<T> extends Consumer<T>, Contextualized, ContextHolder {

}
