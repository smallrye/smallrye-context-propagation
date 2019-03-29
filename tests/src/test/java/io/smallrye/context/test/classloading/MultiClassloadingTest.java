/*
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
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
package io.smallrye.context.test.classloading;

import java.util.Map;
import java.util.function.BiConsumer;

import javax.enterprise.util.AnnotationLiteral;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManager;
import org.eclipse.microprofile.context.spi.ContextManagerExtension;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.classloader.ShrinkWrapClassLoader;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigProviderResolver;
import io.smallrye.context.SmallRyeContextManagerProvider;
import io.smallrye.context.api.ManagedExecutorConfig;
import io.smallrye.context.impl.ThreadContextImpl;

public class MultiClassloadingTest {

    public static class AThreadContextPropagator implements ContextManagerExtension {

        @Override
        public void setup(ContextManager manager) {
            // TODO Auto-generated method stub

        }

    }

    public static class BThreadContextPropagator implements ContextManagerExtension {

        @Override
        public void setup(ContextManager manager) {
            // TODO Auto-generated method stub

        }

    }

    public static class AThreadContextProvider implements ThreadContextProvider {

        @Override
        public ThreadContextSnapshot currentContext(Map<String, String> props) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ThreadContextSnapshot clearedContext(Map<String, String> props) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getThreadContextType() {
            return "A";
        }

    }

    public static class BThreadContextProvider implements ThreadContextProvider {

        @Override
        public ThreadContextSnapshot currentContext(Map<String, String> props) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ThreadContextSnapshot clearedContext(Map<String, String> props) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getThreadContextType() {
            return "B";
        }

    }

    @Test
    public void test() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        String thisPackage = MultiClassloadingTest.class.getPackage().getName();

        JavaArchive parentJar = ShrinkWrap.create(JavaArchive.class).addPackages(true, ThreadContext.class.getPackage().getName())
                .addPackages(true, Assert.class.getPackage().getName())
                .addPackages(true, ConfigProvider.class.getPackage().getName())
                .addPackages(true, SmallRyeConfig.class.getPackage().getName())
                .addPackages(true, Logger.class.getPackage().getName())
                .addPackage(AnnotationLiteral.class.getPackage().getName())
                // done use addPackages for Smallrye-Context because it
                // would include test packages
                .addPackage(SmallRyeContextManagerProvider.class.getPackage().getName())
                .addPackage(ThreadContextImpl.class.getPackage().getName())
                .addPackage(ContextManagerExtension.class.getPackage().getName())
                .addPackage(ManagedExecutorConfig.class.getPackage().getName())
                .addAsServiceProvider(ConfigProviderResolver.class, SmallRyeConfigProviderResolver.class)
                .addAsServiceProvider(ContextManagerProvider.class, SmallRyeContextManagerProvider.class);
        ShrinkWrapClassLoader parentCL = new ShrinkWrapClassLoader((ClassLoader) null, parentJar);
        System.err.println("ParentCL: " + parentCL);

        JavaArchive aJar = ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProviderAndClasses(ThreadContextProvider.class, AThreadContextProvider.class)
                .addAsServiceProviderAndClasses(ContextManagerExtension.class, AThreadContextPropagator.class).addClass(ATest.class);
        ShrinkWrapClassLoader aCL = new ShrinkWrapClassLoader(parentCL, aJar);
        System.err.println("aCL: " + aCL);

        JavaArchive bJar = ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProviderAndClasses(ThreadContextProvider.class, BThreadContextProvider.class)
                .addAsServiceProviderAndClasses(ContextManagerExtension.class, BThreadContextPropagator.class).addClass(BTest.class);
        ShrinkWrapClassLoader bCL = new ShrinkWrapClassLoader(parentCL, bJar);
        System.err.println("bCL: " + bCL);

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(aCL);
            BiConsumer<ClassLoader, ClassLoader> aTest = (BiConsumer<ClassLoader, ClassLoader>) aCL.loadClass(thisPackage + ".ATest")
                    .newInstance();
            aTest.accept(aCL, parentCL);

            Thread.currentThread().setContextClassLoader(bCL);
            BiConsumer<ClassLoader, ClassLoader> bTest = (BiConsumer<ClassLoader, ClassLoader>) bCL.loadClass(thisPackage + ".BTest")
                    .newInstance();
            bTest.accept(bCL, parentCL);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }
}
