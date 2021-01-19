package io.smallrye.context.impl.wrappers;

import java.util.function.Supplier;

import io.smallrye.context.impl.ContextHolder;
import io.smallrye.context.impl.Contextualized;

public interface ContextualSupplier<R> extends Supplier<R>, Contextualized, ContextHolder {

}
