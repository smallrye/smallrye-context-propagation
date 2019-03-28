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
package io.smallrye.context.inject;

import io.smallrye.context.api.ManagedExecutorConfig;
import io.smallrye.context.api.NamedInstance;
import io.smallrye.context.api.ThreadContextConfig;
import io.smallrye.context.impl.ManagedExecutorBuilderImpl;
import io.smallrye.context.impl.ThreadContextBuilderImpl;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CDI extension that takes care of injectable ThreadContext and ManagedExecutor instances.
 *
 * Also takes into consideration MP Config which may be used to override injection point configuration.
 *
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 */
public class SmallryeContextCdiExtension implements Extension {

    private final String nameDelimiter = "/";
    private final String maxAsync = nameDelimiter + "maxAsync";
    private final String maxQueued = nameDelimiter + "maxQueued";
    private final String cleared = nameDelimiter + "cleared";
    private final String propagated = nameDelimiter + "propagated";
    private final String unchanged = nameDelimiter + "unchanged";
    private final String MEConfig = nameDelimiter + "ManagedExecutorConfig";
    private final String TCConfig = nameDelimiter + "ThreadContextConfig";

    // used when adding beans, we need to make sure we have correct configuration, MP config allows to override it
    private final Config mpConfig = ConfigProvider.getConfig();

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
                    .createWith(param -> ((ManagedExecutorBuilderImpl)ManagedExecutor.builder())
                            .injectionPointName(entry.getKey().getMpConfigName())
                            .maxAsync(resolveConfiguration(entry.getKey().getMpConfigName() + MEConfig + maxAsync,
                                    Integer.class, annotation.maxAsync()))
                            .maxQueued(resolveConfiguration(entry.getKey().getMpConfigName() + MEConfig + maxQueued,
                                    Integer.class, annotation.maxQueued()))
                            .cleared(resolveConfiguration(entry.getKey().getMpConfigName() + MEConfig + cleared,
                                    String[].class, annotation.cleared()))
                            .propagated(resolveConfiguration(entry.getKey().getMpConfigName() + MEConfig + propagated,
                                    String[].class, annotation.propagated()))
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
                    .createWith(param -> ((ManagedExecutorBuilderImpl)ManagedExecutor.builder())
                            .injectionPointName(ipName.getMpConfigName())
                            .maxAsync(resolveConfiguration(ipName.getMpConfigName() + MEConfig + maxAsync,
                                    Integer.class, ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.maxAsync()))
                            .maxQueued(resolveConfiguration(ipName.getMpConfigName() + MEConfig + maxQueued,
                                    Integer.class, ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.maxQueued()))
                            .cleared(resolveConfiguration(ipName.getMpConfigName() + MEConfig + cleared,
                                    String[].class, ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.cleared()))
                            .propagated(resolveConfiguration(ipName.getMpConfigName() + MEConfig + propagated,
                                    String[].class, ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.propagated()))
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
                    .createWith(param -> ((ThreadContextBuilderImpl)ThreadContext.builder())
                            .injectionPointName(entry.getKey().getMpConfigName())
                            .cleared(resolveConfiguration(entry.getKey().getMpConfigName() + TCConfig + cleared,
                                    String[].class, annotation.cleared()))
                            .unchanged(resolveConfiguration(entry.getKey().getMpConfigName() + TCConfig + unchanged,
                                    String[].class, annotation.unchanged()))
                            .propagated(resolveConfiguration(entry.getKey().getMpConfigName() + TCConfig + propagated,
                                    String[].class, annotation.propagated()))
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
                    .createWith(param -> ((ThreadContextBuilderImpl)ThreadContext.builder())
                            .injectionPointName(ipName.getMpConfigName())
                            .cleared(resolveConfiguration(ipName.getMpConfigName() + TCConfig + cleared,
                                    String[].class, ThreadContextConfig.Literal.DEFAULT_INSTANCE.cleared()))
                            .unchanged(resolveConfiguration(ipName.getMpConfigName() + TCConfig + unchanged,
                                    String[].class, ThreadContextConfig.Literal.DEFAULT_INSTANCE.unchanged()))
                            .propagated(resolveConfiguration(ipName.getMpConfigName() + TCConfig + propagated,
                                    String[].class, ThreadContextConfig.Literal.DEFAULT_INSTANCE.propagated()))
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
     * Attempts to find MP Config for given String and return type, if not found, returns the default value
     *
     * @param mpConfigName String under which to search in MP Config
     * @param originalValue value to return if no MP Config is found
     * @return value found via MP Config if there is any, defaultValue otherwise
     */
    private <K> K resolveConfiguration(String mpConfigName, Class<K> expectedReturnType, K originalValue) {
        // workaround for https://github.com/smallrye/smallrye-config/issues/83
        // once resolved, we should be using getOptionalValue() as follows:
        //return mpConfig.getOptionalValue(mpConfigName, expectedReturnType).orElse(defaultValue);
        try {
            return mpConfig.getValue(mpConfigName, expectedReturnType);
        } catch (NoSuchElementException e) {
            // ok, MP Conf does not override this property, let's use the original one
            return originalValue;
        }
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