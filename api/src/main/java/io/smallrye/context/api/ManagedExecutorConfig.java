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

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;

/**
 * <p>
 * Annotates a CDI injection point for a {@link ManagedExecutor} such that the container
 * creates a new instance, which is identified within an application by its unique name.
 * The unique name is generated as the fully qualified class name (with each component delimited by <code>.</code>)
 * and the injection point's field name or method name and parameter position, all delimited by <code>/</code>,
 * unless annotated with the {@link NamedInstance} qualifier,
 * in which case the unique name is specified by the {@link NamedInstance#value} attribute of that qualifier.
 * </p>
 *
 * <p>
 * For example, the following injection points share a single
 * {@link ManagedExecutor} instance,
 * </p>
 *
 * <pre>
 * <code> {@literal @}Inject {@literal @}NamedInstance("exec1") {@literal @}ManagedExecutorConfig(maxAsync=5)
 * ManagedExecutor executor;
 *
 * {@literal @}Inject
 * void setCompletableFuture({@literal @}NamedInstance("exec1") ManagedExecutor exec) {
 *     completableFuture = exec.newIncompleteFuture();
 * }
 *
 * {@literal @}Inject
 * void setCompletionStage({@literal @}NamedInstance("exec1") ManagedExecutor exec) {
 *     completionStage = exec.supplyAsync(supplier);
 * }
 * </code>
 * </pre>
 *
 * <p>
 * Alternatively, the following injection points each represent a distinct
 * {@link ManagedExecutor} instance,
 * </p>
 *
 * <pre>
 * <code> {@literal @}Inject {@literal @}ManagedExecutorConfig(propagated=ThreadContext.CDI)
 * ManagedExecutor exec2;
 *
 * {@literal @}Inject {@literal @}ManagedExecutorConfig(maxAsync=5)
 * ManagedExecutor exec3;
 * </code>
 * </pre>
 *
 * <p>
 * When the application stops, the container automatically shuts down instances
 * of {@link ManagedExecutor} that it created. The application can manually use the
 * {@link ManagedExecutor#shutdown} or {@link ManagedExecutor#shutdownNow} methods
 * to shut down a managed executor at an earlier point.
 * </p>
 *
 * <p>
 * A <code>ManagedExecutor</code> will fail to inject, raising
 * {@link javax.enterprise.inject.spi.DefinitionException DefinitionException} on application startup,
 * if multiple injection points are annotated to create instances with the same name.
 * </p>
 *
 * @author Matej Novotny
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER })
public @interface ManagedExecutorConfig {
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
     * set of cleared context if the {@link #propagated} set does not include
     * {@link ThreadContext#ALL_REMAINING}.
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
     * A <code>ManagedExecutor</code> must fail to inject, raising
     * {@link javax.enterprise.inject.spi.DefinitionException DefinitionException}
     * on application startup,
     * if a context type specified within this set is unavailable
     * or if the {@link #propagated} set includes one or more of the
     * same types as this set.
     * </p>
     */
    String[] cleared() default {};

    /**
     * <p>
     * Defines the set of thread context types to capture from the thread
     * that creates a dependent stage (or that submits a task) and which to
     * propagate to the thread where the action or task executes.
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
     * Thread context types which are not otherwise included in this set
     * are cleared from the thread of execution for the duration of the
     * action or task.
     * </p>
     *
     * <p>
     * A <code>ManagedExecutor</code> must fail to inject, raising
     * {@link javax.enterprise.inject.spi.DefinitionException DefinitionException}
     * on application startup,
     * if a context type specified within this set is unavailable
     * or if the {@link #cleared} set includes one or more of the
     * same types as this set.
     * </p>
     */
    String[] propagated() default { ThreadContext.ALL_REMAINING };

    /**
     * <p>
     * Establishes an upper bound on the number of async completion stage
     * actions and async executor tasks that can be running at any given point
     * in time. Async actions and tasks remain queued until
     * the <code>ManagedExecutor</code> starts executing them.
     * </p>
     *
     * <p>
     * The default value of <code>-1</code> indicates no upper bound,
     * although practically, resource constraints of the system will apply.
     * </p>
     *
     * <p>
     * A <code>ManagedExecutor</code> must fail to inject, raising
     * {@link javax.enterprise.inject.spi.DefinitionException DefinitionException}
     * on application startup, if the
     * <code>maxAsync</code> value is 0 or less than -1.
     */
    int maxAsync() default -1;

    /**
     * <p>
     * Establishes an upper bound on the number of async actions and async tasks
     * that can be queued up for execution. Async actions and tasks are rejected
     * if no space in the queue is available to accept them.
     * </p>
     *
     * <p>
     * The default value of <code>-1</code> indicates no upper bound,
     * although practically, resource constraints of the system will apply.
     * </p>
     *
     * <p>
     * A <code>ManagedExecutor</code> must fail to inject, raising
     * {@link javax.enterprise.inject.spi.DefinitionException DefinitionException}
     * on application startup, if the
     * <code>maxQueued</code> value is 0 or less than -1.
     */
    int maxQueued() default -1;

    /**
     * Util class used for inline creation of {@link ManagedExecutorConfig} annotation instances.
     */
    final class Literal extends AnnotationLiteral<ManagedExecutorConfig> implements ManagedExecutorConfig {

        public static final Literal DEFAULT_INSTANCE = of(-1, -1, new String[] {},
                new String[] { ThreadContext.ALL_REMAINING });

        private static final long serialVersionUID = 1L;

        private final int maxAsync;
        private final int maxQueued;
        private final String[] cleared;
        private final String[] propagated;

        private Literal(int maxAsync, int maxQueued, String[] cleared, String[] propagated) {
            this.cleared = cleared;
            this.propagated = propagated;
            this.maxAsync = maxAsync;
            this.maxQueued = maxQueued;
        }

        public static Literal of(int maxAsync, int maxQueued, String[] cleared, String[] propagated) {
            return new Literal(maxAsync, maxQueued, cleared, propagated);
        }

        public int maxAsync() {
            return maxAsync;
        }

        public int maxQueued() {
            return maxQueued;
        }

        public String[] cleared() {
            return cleared;
        }

        public String[] propagated() {
            return propagated;
        }
    }
}
