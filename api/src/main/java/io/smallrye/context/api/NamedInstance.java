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

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>This annotation is used to achieve out of the box {@link ManagedExecutor} and {@link ThreadContext} instance
 * sharing through CDI injection.</p>
 *
 * <p>Qualifies a CDI injection point for a {@link ManagedExecutor} or {@link ThreadContext} with a unique name
 * across the application.</p>
 *
 * <p>This annotation can be used in combination with the {@link ManagedExecutorConfig} or
 * {@link ThreadContextConfig} annotation to define a new instance. For example,</p>
 *
 * <pre><code> {@literal @}Inject {@literal @}NamedInstance("myExecutor") {@literal @}ManagedExecutorConfig(maxAsync=10)
 * ManagedExecutor myExecutor;
 *
 * {@literal @}Inject {@literal @}NamedInstance("myContext") {@literal @}ThreadContextConfig(propagated = { ThreadContext.SECURITY, ThreadContext.CDI })
 * ThreadContext myThreadContext;
 * </code></pre>
 *
 * <p>Once used as shown above, this annotation can be used on its own to qualify an injection point with the name of
 * an existing instance. Injection points with the same {@link NamedInstance#value()} then share the same
 * underlying contextual instance. For example, referencing a name from the previous example,</p>
 *
 * <pre><code> {@literal @}Inject {@literal @}NamedInstance("myExecutor")
 * ManagedExecutor exec1;
 *
 * {@literal @}Inject {@literal @}NamedInstance("myContext")
 * ThreadContext myContextPropagator;
 * </code></pre>
 *
 * <p>Alternatively, an application can use this annotation as a normal CDI qualifier,
 * defining its own scope, producer, and disposer. For example,</p>
 *
 * @author Matej Novotny
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER })
public @interface NamedInstance {
    /**
     * Unique name that qualifies a {@link ManagedExecutor} or {@link ThreadContext}.
     */
    String value();

    /**
     * Supports inline instantiation of the {@link NamedInstance} qualifier.
     */
    final class Literal extends AnnotationLiteral<NamedInstance> implements NamedInstance {

        private static final long serialVersionUID = 1L;
        private final String value;

        private Literal(String value) {
            this.value = value;
        }

        public static Literal of(String value) {
            return new Literal(value);
        }

        public String value() {
            return value;
        }
    }
}
