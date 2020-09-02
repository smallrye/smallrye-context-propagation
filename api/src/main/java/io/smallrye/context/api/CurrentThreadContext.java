/*
 * Copyright (c) 2018,2019 Contributors to the Eclipse Foundation
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
package io.smallrye.context.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

import org.eclipse.microprofile.context.ThreadContext;

/**
 * <p>
 * Declares that a method should be called with its current {@link ThreadContext} set to one created
 * with the specified settings.
 * </p>
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface CurrentThreadContext {

    /**
     * Defines that the current thread context should be removed. This is mutually exclusive
     * with the other settings.
     * 
     * @return a boolean indicating if the current thread context should be removed. Defaults to false.
     */
    @Nonbinding
    public boolean remove() default false;

    /**
     * <p>
     * Defines the set of thread context types to clear from the thread
     * where the action or task executes. The previous context is resumed
     * on the thread after the action or task ends.
     * </p>
     *
     * <p>
     * By default, no context is cleared/suspended from execution thread.
     * </p>
     *
     * <p>
     * {@link ThreadContext#ALL_REMAINING} is automatically appended to the
     * set of cleared context if neither the {@link #propagated} set nor the
     * {@link #unchanged} set include {@link ThreadContext#ALL_REMAINING}.
     * </p>
     *
     * <p>
     * Constants for specifying some of the core context types are provided
     * on {@link ThreadContext}. Other thread context types must be defined
     * by the specification that defines the context type or by a related
     * MicroProfile specification.
     * </p>
     *
     * <p>
     * A <code>CurrentThreadContext</code> must fail to initialise, raising
     * {@link javax.enterprise.inject.spi.DefinitionException DefinitionException}
     * on application startup,
     * if a context type specified within this set is unavailable
     * or if the {@link #propagated} and/or {@link #unchanged} set
     * includes one or more of the same types as this set.
     * </p>
     *
     * @return an array of strings of thread context types to clear.
     */
    @Nonbinding
    String[] cleared() default {};

    /**
     * <p>
     * Defines the set of thread context types to capture from the thread
     * that contextualizes an action or task. This context is later
     * re-established on the thread(s) where the action or task executes.
     * </p>
     *
     * <p>
     * The default set of propagated thread context types is
     * {@link ThreadContext#ALL_REMAINING}, which includes all available
     * thread context types that support capture and propagation to other
     * threads, except for those that are explicitly {@code cleared}.
     * </p>
     *
     * <p>
     * Constants for specifying some of the core context types are provided
     * on {@link ThreadContext}. Other thread context types must be defined
     * by the specification that defines the context type or by a related
     * MicroProfile specification.
     * </p>
     *
     * <p>
     * Thread context types which are not otherwise included in this set or
     * in the {@link #unchanged} set are cleared from the thread of execution
     * for the duration of the action or task.
     * </p>
     *
     * <p>
     * A <code>CurrentThreadContext</code> must fail to initialise, raising
     * {@link javax.enterprise.inject.spi.DefinitionException DefinitionException}
     * on application startup,
     * if a context type specified within this set is unavailable
     * or if the {@link #cleared} and/or {@link #unchanged} set
     * includes one or more of the same types as this set.
     * </p>
     *
     * @return an array of strings of thread context types to propagate.
     */
    @Nonbinding
    String[] propagated() default { ThreadContext.ALL_REMAINING };

    /**
     * <p>
     * Defines a set of thread context types that are essentially ignored,
     * in that they are neither captured nor are they propagated or cleared
     * from thread(s) that execute the action or task.
     * </p>
     *
     * <p>
     * Constants for specifying some of the core context types are provided
     * on {@link ThreadContext}. Other thread context types must be defined
     * by the specification that defines the context type or by a related
     * MicroProfile specification.
     *
     * <p>
     * The configuration <code>unchanged</code> context is provided for
     * advanced patterns where it is desirable to leave certain context types
     * on the executing thread.
     * </p>
     *
     * <p>
     * For example, to run as the current application, but under the
     * transaction of the thread where the task executes:
     * </p>
     * 
     * <pre>
     * <code> {@literal @}WithThreadContext(unchanged = ThreadContext.TRANSACTION,
     *                              propagated = ThreadContext.APPLICATION,
     *                              cleared = ThreadContext.ALL_REMAINING)
     * public void method() {
     *   ...
     *   task = SmallRyeThreadContext.getCurrentThreadContext().contextualRunnable(new MyTransactionalTask());
     *   ...
     *   // on another thread,
     *   tx.begin();
     *   ...
     *   task.run(); // runs under the transaction due to 'unchanged'
     *   tx.commit();
     * }
     * </code>
     * </pre>
     *
     * <p>
     * A <code>CurrentThreadContext</code> must fail to initialise, raising
     * {@link javax.enterprise.inject.spi.DefinitionException DefinitionException}
     * on application startup,
     * if a context type specified within this set is unavailable
     * or if the {@link #cleared} and/or {@link #propagated} set
     * includes one or more of the same types as this set.
     * </p>
     *
     * @return an array of strings of thread context types to be ignored.
     */
    @Nonbinding
    String[] unchanged() default {};

    /**
     * Util class used for inline creation of {@link CurrentThreadContext} annotation instances.
     */
    final class Literal extends AnnotationLiteral<CurrentThreadContext> implements CurrentThreadContext {

        public static final Literal DEFAULT_INSTANCE = of(false, new String[] {}, new String[] {},
                new String[] { ThreadContext.ALL_REMAINING });

        private static final long serialVersionUID = 1L;

        private final boolean remove;
        private final String[] cleared;
        private final String[] unchanged;
        private final String[] propagated;

        private Literal(boolean remove, String[] cleared, String[] unchanged, String[] propagated) {
            this.remove = remove;
            this.cleared = cleared;
            this.unchanged = unchanged;
            this.propagated = propagated;
        }

        public static Literal of(boolean remove, String[] cleared, String[] unchanged, String[] propagated) {
            return new Literal(remove, cleared, unchanged, propagated);
        }

        @Override
        public boolean remove() {
            return remove;
        }

        @Override
        public String[] cleared() {
            return cleared;
        }

        @Override
        public String[] unchanged() {
            return unchanged;
        }

        @Override
        public String[] propagated() {
            return propagated;
        }
    }
}
