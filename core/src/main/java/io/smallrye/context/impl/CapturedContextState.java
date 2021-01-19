package io.smallrye.context.impl;

import io.smallrye.context.CleanAutoCloseable;

@FunctionalInterface
public interface CapturedContextState {
    CleanAutoCloseable begin();
}
