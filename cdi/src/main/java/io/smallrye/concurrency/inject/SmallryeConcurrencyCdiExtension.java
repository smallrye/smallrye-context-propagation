/*
 * Copyright 2018 <a href="mailto:manovotn@redhat.com">Matej Novotny</a>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.concurrency.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.*;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.ManagedExecutorConfig;
import org.eclipse.microprofile.concurrent.NamedInstance;
import org.eclipse.microprofile.concurrent.ThreadContext;
import org.eclipse.microprofile.concurrent.ThreadContextConfig;

/**
 * CDI extension that takes care of injectable ThreadContext and ManagedExecutor instances.
 *
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 */
public class SmallryeConcurrencyCdiExtension implements Extension {

    private Map<String, ManagedExecutorConfig> executorMap = new HashMap<>();
    private Set<String> unconfiguredExecutorIPs = new HashSet<>();
    private Map<String, ThreadContextConfig> threadContextMap = new HashMap<>();
    private Set<String> unconfiguredContextIPs = new HashSet<>();

    private Set<String> userDefinedMEProducers = new HashSet<>();
    private Set<String> userDefinedTCProducers = new HashSet<>();

    public void processInjectionPointME(@Observes ProcessInjectionPoint<?, ManagedExecutor> pip) {
        InjectionPoint ip = pip.getInjectionPoint();
        // get the unique name from @NamedInstance if present
        String uniqueName = getUniqueName(ip);
        if (uniqueName == null) {
            // no explicit name, we infer an implicit one as declaringClassName and field/param name separated by comma
            uniqueName = ip.getMember().getDeclaringClass().toString() + "." + ip.getMember().toString();
            // add @NamedInstance with the generated name
            pip.configureInjectionPoint().addQualifier(NamedInstance.Literal.of(uniqueName));
        }
        // extract the config annotation
        ManagedExecutorConfig annotation = extractAnnotationFromIP(ip, ManagedExecutorConfig.class);
        ManagedExecutorConfig previousValue = null;
        if (annotation == null) {
            // no config exists, store into unconfigured for now
            unconfiguredExecutorIPs.add(uniqueName);
        } else {
            previousValue = executorMap.putIfAbsent(uniqueName, annotation);
        }
        if (previousValue != null) {
            // TODO handle clashes, probably store in some other map and then blow up with complete information across all IPs?
        }
    }

    public void processInjectionPointTC(@Observes ProcessInjectionPoint<?, ThreadContext> pip) {
        InjectionPoint ip = pip.getInjectionPoint();
        // get the unique name from @NamedInstance if present
        String uniqueName = getUniqueName(ip);
        if (uniqueName == null) {
            // no explicit name, we infer an implicit one as declaringClassName and field/param name separated by comma
            uniqueName = ip.getMember().getDeclaringClass().toString() + "." + ip.getMember().toString();
            // add @NamedInstance with the generated name
            pip.configureInjectionPoint().addQualifier(NamedInstance.Literal.of(uniqueName));
        }
        // extract the config annotation
        ThreadContextConfig annotation = extractAnnotationFromIP(ip, ThreadContextConfig.class);
        ThreadContextConfig previousValue = null;
        if (annotation == null) {
            // no config exists, store into unconfigured for now
            unconfiguredContextIPs.add(uniqueName);
        } else {
            previousValue = threadContextMap.putIfAbsent(uniqueName, annotation);
        }
        if (previousValue != null) {
            // TODO handle clashes, probably store in some other map and then blow up with complete information across all IPs?
        }
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery abd) {
        // check all unconfigured IPs, if we also found same name and configured ones, then drop these from the set
        unconfiguredExecutorIPs.removeAll(unconfiguredExecutorIPs.stream()
            .filter((name) -> (executorMap.containsKey(name)))
            .collect(Collectors.toSet()));

        unconfiguredContextIPs.removeAll(unconfiguredContextIPs.stream()
            .filter((name) -> (threadContextMap.containsKey(name)))
            .collect(Collectors.toSet()));

        // we also need to remove all that we found a user producer for
        unconfiguredExecutorIPs.removeAll(userDefinedMEProducers);
        unconfiguredContextIPs.removeAll(userDefinedTCProducers);

        // remove information about ME and TC that user defined producers for
        for (String s: userDefinedMEProducers) {
            executorMap.remove(s);
        }
        for (String s : userDefinedTCProducers) {
            threadContextMap.remove(s);
        }

        // add beans for configured ManagedExecutors
        for (Map.Entry<String, ManagedExecutorConfig> entry : executorMap.entrySet()) {
            ManagedExecutorConfig annotation = entry.getValue();
            abd.<ManagedExecutor>addBean()
                .beanClass(ManagedExecutor.class)
                .addTransitiveTypeClosure(ManagedExecutor.class)
                .addQualifier(NamedInstance.Literal.of(entry.getKey()))
                .scope(ApplicationScoped.class)
                .disposeWith((ManagedExecutor t, Instance<Object> u) -> {
                    // bean is ApplicationScoped, ME.shutdown() is called only after whole app is being shutdown
                    t.shutdown();
                })
                .createWith(param -> ManagedExecutor.builder()
                    .maxAsync(annotation.maxAsync())
                    .maxQueued(annotation.maxQueued())
                    .cleared(annotation.cleared())
                    .propagated(annotation.propagated())
                    .build());
        }
        // add beans for unconfigured ManagedExecutors
        for (String name : unconfiguredExecutorIPs) {
            abd.<ManagedExecutor>addBean()
                .beanClass(ManagedExecutor.class)
                .addTransitiveTypeClosure(ManagedExecutor.class)
                .addQualifier(NamedInstance.Literal.of(name))
                .scope(ApplicationScoped.class)
                .disposeWith((ManagedExecutor t, Instance<Object> u) -> {
                    // bean is ApplicationScoped, ME.shutdown() is called only after whole app is being shutdown
                    t.shutdown();
                })
                .createWith(param -> ManagedExecutor.builder()
                    .maxAsync(ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.maxAsync())
                    .maxQueued(ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.maxQueued())
                    .cleared(ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.cleared())
                    .propagated(ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.propagated())
                    .build());
        }
        // add beans for configured ThreadContext
        for (Map.Entry<String, ThreadContextConfig> entry : threadContextMap.entrySet()) {
            ThreadContextConfig annotation = entry.getValue();
            abd.<ThreadContext>addBean()
                .beanClass(ThreadContext.class)
                .addTransitiveTypeClosure(ThreadContext.class)
                .addQualifier(NamedInstance.Literal.of(entry.getKey()))
                .scope(ApplicationScoped.class)
                .disposeWith((ThreadContext t, Instance<Object> u) -> {
                    // no-op at this point
                })
                .createWith(param -> ThreadContext.builder()
                    .cleared(annotation.cleared())
                    .unchanged(annotation.unchanged())
                    .propagated(annotation.propagated())
                    .build());
        }
        // add beans for unconfigured ThreadContext
        for (String name : unconfiguredContextIPs) {
            abd.<ThreadContext>addBean()
                .beanClass(ThreadContext.class)
                .addTransitiveTypeClosure(ThreadContext.class)
                .addQualifier(NamedInstance.Literal.of(name))
                .scope(ApplicationScoped.class)
                .disposeWith((ThreadContext t, Instance<Object> u) -> {
                    // no-op
                })
                .createWith(param -> ThreadContext.builder()
                    .cleared(ThreadContextConfig.Literal.DEFAULT_INSTANCE.cleared())
                    .unchanged(ThreadContextConfig.Literal.DEFAULT_INSTANCE.unchanged())
                    .propagated(ThreadContextConfig.Literal.DEFAULT_INSTANCE.propagated())
                    .build());
        }
    }

    public void processThreadContextProducers(@Observes ProcessProducer<?, ThreadContext> processProducer) {
        NamedInstance annotation = null;
        Member javaMember = processProducer.getAnnotatedMember().getJavaMember();
        if (javaMember instanceof Method) {
            // producer method
            annotation = ((Method) javaMember).getAnnotation(NamedInstance.class);
        } else {
            if (javaMember instanceof Field) {
                // field producer
                annotation = ((Field) javaMember).getAnnotation(NamedInstance.class);
            }
        }
        if (annotation != null) {
            userDefinedTCProducers.add(annotation.value());
        }
    }

    public void processMEProducers(@Observes ProcessProducer<?, ManagedExecutor> processProducer) {
        NamedInstance annotation = null;
        Member javaMember = processProducer.getAnnotatedMember().getJavaMember();
        if (javaMember instanceof Method) {
            // producer method
            annotation = ((Method) javaMember).getAnnotation(NamedInstance.class);
        } else {
            if (javaMember instanceof Field) {
                // field producer
                annotation = ((Field) javaMember).getAnnotation(NamedInstance.class);
            }
        }
        if (annotation != null) {
            userDefinedMEProducers.add(annotation.value());
        }
    }

    /**
     * Extracts a {@link ManagedExecutorConfig} or {@link ThreadContextConfig} annotation from an {@link InjectionPoint} for
     * further processing. Can return null which indicates no such annotation is present (default configuration or simply a
     * named instance).
     *
     * @param ip
     * @return extracted {@link ManagedExecutorConfig} or {@link ThreadContextConfig} annotation or null if it is not present
     */
    private <T extends Annotation> T extractAnnotationFromIP(InjectionPoint injectionPoint, Class<T> annotationClazz) {
        T annotation = null;
        if (!injectionPoint.getQualifiers().isEmpty()) {
            Member member = injectionPoint.getMember();
            if (member instanceof Field) {
                // injection into field
                annotation = ((Field) member).getAnnotation(annotationClazz);
            } else {
                if (member instanceof Method) {
                    //injection into method
                    annotation = ((Method) member).getAnnotation(annotationClazz);
                } else {
                    // constructor injection
                    annotation = ((Constructor<?>) member).getAnnotation(annotationClazz);
                }
            }
        }
        return annotation;
    }

    private String getUniqueName(InjectionPoint ip) {
        Optional<NamedInstance> optionalQulifier = ip.getQualifiers().stream().filter(ann -> ann.annotationType().equals(NamedInstance.class))
            .map(ann -> (NamedInstance) ann) // not a repeateble annotation, finding first will suffice
            .findFirst();
        return (optionalQulifier.isPresent()) ? optionalQulifier.get().value() : null;
    }
}
