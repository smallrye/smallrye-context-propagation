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
package io.smallrye.concurrency.test.classloading;

import java.util.Map;
import java.util.function.BiConsumer;

import org.eclipse.microprofile.concurrent.ThreadContext;
import org.eclipse.microprofile.concurrent.spi.ConcurrencyManager;
import org.eclipse.microprofile.concurrent.spi.ConcurrencyManagerExtension;
import org.eclipse.microprofile.concurrent.spi.ConcurrencyProvider;
import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;
import org.eclipse.microprofile.concurrent.spi.ThreadContextSnapshot;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.classloader.ShrinkWrapClassLoader;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;

import io.smallrye.concurrency.SmallRyeConcurrencyProvider;
import io.smallrye.concurrency.impl.ThreadContextImpl;

public class MultiClassloadingTest {

    public static class AThreadContextPropagator implements ConcurrencyManagerExtension {

        @Override
        public void setup(ConcurrencyManager manager) {
            // TODO Auto-generated method stub

        }

    }

    public static class BThreadContextPropagator implements ConcurrencyManagerExtension {

        @Override
        public void setup(ConcurrencyManager manager) {
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
                // done use addPackages for Smallrye-Concurrency because it
                // would include test packages
                .addPackage(SmallRyeConcurrencyProvider.class.getPackage().getName())
                .addPackage(ThreadContextImpl.class.getPackage().getName())
                .addPackage(ConcurrencyManagerExtension.class.getPackage().getName())
                .addAsServiceProvider(ConcurrencyProvider.class, SmallRyeConcurrencyProvider.class);
        ShrinkWrapClassLoader parentCL = new ShrinkWrapClassLoader((ClassLoader) null, parentJar);
        System.err.println("ParentCL: " + parentCL);

        JavaArchive aJar = ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProviderAndClasses(ThreadContextProvider.class, AThreadContextProvider.class)
                .addAsServiceProviderAndClasses(ConcurrencyManagerExtension.class, AThreadContextPropagator.class).addClass(ATest.class);
        ShrinkWrapClassLoader aCL = new ShrinkWrapClassLoader(parentCL, aJar);
        System.err.println("aCL: " + aCL);

        JavaArchive bJar = ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProviderAndClasses(ThreadContextProvider.class, BThreadContextProvider.class)
                .addAsServiceProviderAndClasses(ConcurrencyManagerExtension.class, BThreadContextPropagator.class).addClass(BTest.class);
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
