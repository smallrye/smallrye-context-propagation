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

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.ManagedExecutorConfig;
import org.eclipse.microprofile.concurrent.NamedInstance;
import org.eclipse.microprofile.concurrent.ThreadContext;
import org.eclipse.microprofile.concurrent.ThreadContextConfig;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessProducer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CDI extension that takes care of injectable ThreadContext and ManagedExecutor instances.
 *
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 */
public class SmallryeConcurrencyCdiExtension implements Extension {

    private final String nameDelimiter = ".";
    private final String maxAsync = nameDelimiter + "maxAsync";
    private final String maxQueued = nameDelimiter + "maxQueued";
    private final String cleared = nameDelimiter + "cleared";
    private final String propagated = nameDelimiter + "propagated";
    private final String unchanged = nameDelimiter + "unchanged";

    private Map<InjectionPointName, ManagedExecutorConfig> executorMap = new HashMap<>();
    private Set<InjectionPointName> unconfiguredExecutorIPs = new HashSet<>();
    private Map<InjectionPointName, ThreadContextConfig> threadContextMap = new HashMap<>();
    private Set<InjectionPointName> unconfiguredContextIPs = new HashSet<>();

    private Set<InjectionPointName> userDefinedMEProducers = new HashSet<>();
    private Set<InjectionPointName> userDefinedTCProducers = new HashSet<>();

    public void processInjectionPointME(@Observes ProcessInjectionPoint<?, ManagedExecutor> pip) {
        InjectionPoint ip = pip.getInjectionPoint();
        // user-defined IP with qualifiers other than @NamedInstance are not our concern, skip it
        if (hasCustomQualifiers(ip)) {
            return;
        }
        // get the unique name from @NamedInstance if present
        String uniqueName = getUniqueName(ip);
        String mpConfigIpName = createUniqueName(ip);
        if (uniqueName == null) {
            // no explicit name, we infer an implicit one as declaringClassName and field/param name separated by comma
            uniqueName = mpConfigIpName;
            // add @NamedInstance with the generated name
            pip.configureInjectionPoint().addQualifier(NamedInstance.Literal.of(uniqueName));
        }
        InjectionPointName injectionPointName = new InjectionPointName(uniqueName, mpConfigIpName);
        // extract the config annotation
        ManagedExecutorConfig annotation = extractAnnotationFromIP(ip, ManagedExecutorConfig.class);
        ManagedExecutorConfig previousValue = null;
        if (annotation == null) {
            // no config exists, store into unconfigured for now
            unconfiguredExecutorIPs.add(injectionPointName);
        } else {
            previousValue = executorMap.putIfAbsent(injectionPointName, annotation);
        }
        if (previousValue != null) {
            // TODO handle clashes, probably store in some other map and then blow up with complete information across all IPs?
        }
    }

    public void processInjectionPointTC(@Observes ProcessInjectionPoint<?, ThreadContext> pip) {
        InjectionPoint ip = pip.getInjectionPoint();
        // user-defined IP with qualifiers other than @NamedInstance are not our concern, skip it
        if (hasCustomQualifiers(ip)) {
            return;
        }
        // get the unique name from @NamedInstance if present
        String uniqueName = getUniqueName(ip);
        String mpConfigIpName = createUniqueName(ip);
        if (uniqueName == null) {
            // no explicit name, we infer an implicit one as declaringClassName and field/param name separated by comma
            uniqueName = mpConfigIpName;
            // add @NamedInstance with the generated name
            pip.configureInjectionPoint().addQualifier(NamedInstance.Literal.of(uniqueName));
        }
        InjectionPointName injectionPointName = new InjectionPointName(uniqueName, mpConfigIpName);
        // extract the config annotation
        ThreadContextConfig annotation = extractAnnotationFromIP(ip, ThreadContextConfig.class);
        ThreadContextConfig previousValue = null;
        if (annotation == null) {
            // no config exists, store into unconfigured for now
            unconfiguredContextIPs.add(injectionPointName);
        } else {
            previousValue = threadContextMap.putIfAbsent(injectionPointName, annotation);
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
        for (InjectionPointName s : userDefinedMEProducers) {
            executorMap.remove(s);
        }
        for (InjectionPointName s : userDefinedTCProducers) {
            threadContextMap.remove(s);
        }

        // before adding beans, we need to make sure we have correct configuration, MP config allows to override it
        Config mpConfig = ConfigProvider.getConfig();

        // add beans for configured ManagedExecutors
        for (Map.Entry<InjectionPointName, ManagedExecutorConfig> entry : executorMap.entrySet()) {
            ManagedExecutorConfig annotation = entry.getValue();
            abd.<ManagedExecutor>addBean()
                    .beanClass(ManagedExecutor.class)
                    .addTransitiveTypeClosure(ManagedExecutor.class)
                    .addQualifier(NamedInstance.Literal.of(entry.getKey().getNamedInstanceName()))
                    .scope(ApplicationScoped.class)
                    .disposeWith((ManagedExecutor t, Instance<Object> u) -> {
                        // bean is ApplicationScoped, ME.shutdown() is called only after whole app is being shutdown
                        t.shutdown();
                    })
                    .createWith(param -> ManagedExecutor.builder()
                            .maxAsync(mpConfig.getOptionalValue(entry.getKey().getMpConfigName() + maxAsync, Integer.class)
                                    .orElse(annotation.maxAsync()))
                            .maxQueued(mpConfig.getOptionalValue(entry.getKey().getMpConfigName() + maxQueued, Integer.class)
                                    .orElse(annotation.maxQueued()))
                            .cleared(mpConfig.getOptionalValue(entry.getKey().getMpConfigName() + cleared, String[].class)
                                    .orElse(annotation.cleared()))
                            .propagated(mpConfig.getOptionalValue(entry.getKey().getMpConfigName() + propagated, String[].class)
                                    .orElse(annotation.propagated()))
                            .build());
        }
        // add beans for unconfigured ManagedExecutors
        for (InjectionPointName ipName : unconfiguredExecutorIPs) {
            abd.<ManagedExecutor>addBean()
                    .beanClass(ManagedExecutor.class)
                    .addTransitiveTypeClosure(ManagedExecutor.class)
                    .addQualifier(NamedInstance.Literal.of(ipName.getNamedInstanceName()))
                    .scope(ApplicationScoped.class)
                    .disposeWith((ManagedExecutor t, Instance<Object> u) -> {
                        // bean is ApplicationScoped, ME.shutdown() is called only after whole app is being shutdown
                        t.shutdown();
                    })
                    .createWith(param -> ManagedExecutor.builder()
                            .maxAsync(mpConfig.getOptionalValue(ipName.getMpConfigName() + maxAsync, Integer.class)
                                    .orElse(ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.maxAsync()))
                            .maxQueued(mpConfig.getOptionalValue(ipName.getMpConfigName() + maxQueued, Integer.class)
                                    .orElse(ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.maxQueued()))
                            .cleared(mpConfig.getOptionalValue(ipName.getMpConfigName() + cleared, String[].class)
                                    .orElse(ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.cleared()))
                            .propagated(mpConfig.getOptionalValue(ipName.getMpConfigName() + propagated, String[].class)
                                    .orElse(ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.propagated()))
                            .build());
        }
        // add beans for configured ThreadContext
        for (Map.Entry<InjectionPointName, ThreadContextConfig> entry : threadContextMap.entrySet()) {
            ThreadContextConfig annotation = entry.getValue();
            abd.<ThreadContext>addBean()
                    .beanClass(ThreadContext.class)
                    .addTransitiveTypeClosure(ThreadContext.class)
                    .addQualifier(NamedInstance.Literal.of(entry.getKey().getNamedInstanceName()))
                    .scope(ApplicationScoped.class)
                    .disposeWith((ThreadContext t, Instance<Object> u) -> {
                        // no-op at this point
                    })
                    .createWith(param -> ThreadContext.builder()
                            .cleared(mpConfig.getOptionalValue(entry.getKey().getMpConfigName() + cleared, String[].class)
                                    .orElse(annotation.cleared()))
                            .unchanged(mpConfig.getOptionalValue(entry.getKey().getMpConfigName() + unchanged, String[].class)
                                    .orElse(annotation.unchanged()))
                            .propagated(mpConfig.getOptionalValue(entry.getKey().getMpConfigName() + propagated, String[].class)
                                    .orElse(annotation.propagated()))
                            .build());
        }

        // add beans for unconfigured ThreadContext
        for (InjectionPointName ipName : unconfiguredContextIPs) {
            abd.<ThreadContext>addBean()
                    .beanClass(ThreadContext.class)
                    .addTransitiveTypeClosure(ThreadContext.class)
                    .addQualifier(NamedInstance.Literal.of(ipName.getNamedInstanceName()))
                    .scope(ApplicationScoped.class)
                    .disposeWith((ThreadContext t, Instance<Object> u) -> {
                        // no-op
                    })
                    .createWith(param -> ThreadContext.builder()
                            .cleared(mpConfig.getOptionalValue(ipName.getMpConfigName() + cleared, String[].class)
                                    .orElse(ThreadContextConfig.Literal.DEFAULT_INSTANCE.cleared()))
                            .unchanged(mpConfig.getOptionalValue(ipName.getMpConfigName() + unchanged, String[].class)
                                    .orElse(ThreadContextConfig.Literal.DEFAULT_INSTANCE.unchanged()))
                            .propagated(mpConfig.getOptionalValue(ipName.getMpConfigName() + propagated, String[].class)
                                    .orElse(ThreadContextConfig.Literal.DEFAULT_INSTANCE.propagated()))
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
            userDefinedTCProducers.add(new InjectionPointName(annotation.value()));
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
            userDefinedMEProducers.add(new InjectionPointName(annotation.value()));
        }
    }

    /**
     * Extracts a {@link ManagedExecutorConfig} or {@link ThreadContextConfig} annotation from an {@link InjectionPoint} for
     * further processing. Can return null which indicates no such annotation is present (default configuration or simply a
     * named instance).
     *
     * @param injectionPoint  {@link InjectionPoint}
     * @param annotationClazz class of the annotation we want to extract
     * @return extracted {@link ManagedExecutorConfig} or {@link ThreadContextConfig} annotation or null if it is not present
     */
    private <T extends Annotation> T extractAnnotationFromIP(InjectionPoint injectionPoint, Class<T> annotationClazz) {
        T annotation = null;
        if (injectionPoint.getAnnotated().isAnnotationPresent(annotationClazz)) {
            annotation = injectionPoint.getAnnotated().getAnnotation(annotationClazz);
        }
        return annotation;
    }

    private String getUniqueName(InjectionPoint ip) {
        Optional<NamedInstance> optionalQulifier = ip.getQualifiers().stream().filter(ann -> ann.annotationType().equals(NamedInstance.class))
                .map(ann -> (NamedInstance) ann) // not a repeateble annotation, finding first will suffice
                .findFirst();
        return (optionalQulifier.isPresent()) ? optionalQulifier.get().value() : null;
    }

    private String createUniqueName(InjectionPoint ip) {
        StringBuilder builder = new StringBuilder(ip.getMember().getDeclaringClass().getName() //full class name
                + nameDelimiter // delimited as per specification
                + ip.getMember().getName()); //name of field/method
        Annotated annotated = ip.getAnnotated();
        if (annotated instanceof AnnotatedParameter) {
            builder.append(nameDelimiter + (((AnnotatedParameter) annotated).getPosition() + 1)); // delimiter + parameter position
        }
        String result = builder.toString();
        return result;
    }

    private boolean hasCustomQualifiers(InjectionPoint ip) {
        // check whether there is any other qualifier then NamedInstance/Any/Default
        return ip.getQualifiers().stream().anyMatch(ann -> !ann.annotationType().equals(NamedInstance.class)
                && !ann.annotationType().equals(Default.class) && !ann.annotationType().equals(Any.class));

    }

    /**
     * cleans all the metadata we gathered during bootstrap
     */
    public void cleanup(@Observes AfterDeploymentValidation adv) {
        // clear() all the collections we operated on
        this.unconfiguredContextIPs.clear();
        this.unconfiguredExecutorIPs.clear();
        this.userDefinedMEProducers.clear();
        this.userDefinedTCProducers.clear();
        this.executorMap.clear();
        this.threadContextMap.clear();
    }
}