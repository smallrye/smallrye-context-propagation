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

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;

/**
 * Arquillian loadable extension which makes sure
 * {@link SmallRyeContextArchiveProcessor} is registered
 *
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 */
public class SmallRyeContextLoadableExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(ApplicationArchiveProcessor.class, SmallRyeContextArchiveProcessor.class);
    }

}
