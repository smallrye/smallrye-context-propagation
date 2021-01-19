package io.smallrye.context.impl.wrappers;

import java.util.concurrent.Executor;

import io.smallrye.context.impl.ContextHolder;
import io.smallrye.context.impl.Contextualized;

public interface ContextualExecutor extends Executor, Contextualized, ContextHolder {

}
