package io.smallrye.context.impl.wrappers;

import java.util.concurrent.Callable;

import io.smallrye.context.impl.ContextHolder;
import io.smallrye.context.impl.Contextualized;

public interface ContextualCallable<T> extends Callable<T>, Contextualized, ContextHolder {

}
