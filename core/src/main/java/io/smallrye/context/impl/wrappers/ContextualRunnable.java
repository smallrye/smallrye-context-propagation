package io.smallrye.context.impl.wrappers;

import io.smallrye.context.impl.ContextHolder;
import io.smallrye.context.impl.Contextualized;

public interface ContextualRunnable extends Runnable, Contextualized, ContextHolder {

}
