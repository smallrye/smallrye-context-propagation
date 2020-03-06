/*
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.context.spi;

import java.util.concurrent.Callable;

import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

/**
 * An extension of {code ThreadContextSnapshot} which enables the snapshot to
 * perform propagation by wrapping the task.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@FunctionalInterface
public interface WrappingThreadContextSnapshot extends ThreadContextSnapshot {

    /**
     * Does this snapshot need to wrap the underlying task instead of directly
     * manipulating ThreadLocals.
     *
     * @return {@code true} if this snapshot needs to wrap the underlying task.
     */
    default boolean needsToWrap() {
        return false;
    }

    /**
     * Wrap the provided task to ensure the context being propagated is correctly
     * established and cleared as the underlying task is called.
     *
     * @param task the task to wrap.
     * @return the wrapped task.
     */
    default <T> Callable<T> wrap(Callable<T> callable) {
        return callable;
    }

}
