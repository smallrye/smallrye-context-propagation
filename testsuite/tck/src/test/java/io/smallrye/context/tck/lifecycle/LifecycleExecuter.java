/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.context.tck.lifecycle;

import java.lang.reflect.Method;

import org.jboss.arquillian.container.spi.event.container.AfterDeploy;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.container.spi.event.container.BeforeUnDeploy;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.container.ClassContainer;
import org.jnp.server.NamingBeanImpl;

import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.jta.utils.JNDIManager;

import io.smallrye.context.jta.context.propagation.JtaContextProvider;

/**
 * LifecycleExecuter
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class LifecycleExecuter {
    private static final String txnStoreLocation = "target/tx-object-store";
    private NamingBeanImpl namingBean;

    private void registerJTA() throws Exception {
        // Start a JNDI server
        namingBean = new NamingBeanImpl();
        namingBean.start();

        // Bind the JTA implementation to the correct JNDI contexts
        JNDIManager.bindJTAImplementation();

        // Set object store location
        arjPropertyManager.getObjectStoreEnvironmentBean().setObjectStoreDir(txnStoreLocation);
    }

    private void unregisterJTA() throws Exception {
        namingBean.stop();
    }

    public void executeBeforeDeploy(@Observes BeforeDeploy event, TestClass testClass) {
        Archive<?> jar = event.getDeployment().getArchive();
        if (jar instanceof ClassContainer<?>) {
            ((ClassContainer<?>) jar).addClass(JtaContextProvider.LifecycleManager.class);
        }
    }

    public void executeAfterDeploy(@Observes AfterDeploy event, TestClass testClass) throws Exception {
        registerJTA();
        execute("AfterDeploy", testClass.getMethods(io.smallrye.context.tck.lifecycle.api.AfterDeploy.class));
    }

    public void executeBeforeUnDeploy(@Observes BeforeUnDeploy event, TestClass testClass) throws Exception {
        unregisterJTA();
        execute("BeforeUnDeploy", testClass.getMethods(io.smallrye.context.tck.lifecycle.api.BeforeUnDeploy.class));
    }

    /*
     * public void executeAfterUnDeploy(@Observes AfterUnDeploy event, TestClass testClass) {
     * execute(testClass.getMethods(io.smallrye.context.tck.lifecycle.api.AfterUnDeploy.class));
     * }
     */

    private void execute(String msg, Method[] methods) {
        if (methods == null) {
            return;
        }
        for (Method method : methods) {
            try {
                method.invoke(null);
            } catch (Exception e) {
                throw new RuntimeException(msg + ":  Could not execute method: " + method, e);
            }
        }
    }
}
