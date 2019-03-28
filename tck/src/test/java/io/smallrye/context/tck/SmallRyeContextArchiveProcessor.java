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
package io.smallrye.context.tck;

import java.io.File;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

/**
 * We need to add SmallRye implementation into each TCK test archive, that way
 * all added producers and such will work.
 *
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 */
public class SmallRyeContextArchiveProcessor implements ApplicationArchiveProcessor {

    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        // SmallRye Conc. CDI impl
        String dependencyToAdd = "io.smallrye:smallrye-context-propagation-cdi";
        File[] dependencies = Maven.resolver().loadPomFromFile(new File("pom.xml")).resolve(dependencyToAdd).withTransitivity().asFile();
        // in case to WAR, we add it as a library, as would user in their app
        if (applicationArchive instanceof WebArchive) {
            WebArchive archive = (WebArchive) applicationArchive;
            // we need to create new archives and add them as dependencies, they
            // have to be instance of ArchiveAsset in order to pass
            // this check
            // https://github.com/arquillian/arquillian-container-weld/blob/2.0.0.Final/impl/src/main/java/org/jboss/arquillian/container/weld/embedded/Utils.java#L73
            for (int i = 0; i < dependencies.length; i++) {
                JavaArchive newJar = ShrinkWrap.createFromZipFile(JavaArchive.class, dependencies[i]);
                archive.addAsLibrary(newJar);
            }

            archive.addAsResource("jndi.properties");
            archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
            archive.addAsManifestResource("META-INF/services", ArchivePaths.create("org.jboss.weld.bootstrap.api.Service"));
        }
        if (applicationArchive instanceof JavaArchive) {
            // TODO, add impl for JARs, we would need to add them as packages or
            // classes
        }
    }

}
