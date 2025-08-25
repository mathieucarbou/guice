/*
 * Copyright (C) 2010-2025 Mycila (mathieu.carbou@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mycila.guice.ext.jsr250;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Stage;
import com.mycila.guice.ext.closeable.CloseableInjector;
import com.mycila.guice.ext.closeable.CloseableModule;
import org.junit.Test;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

public class MycilaDestroyOrderTest {

    @Singleton
    public static class Repository {
        private boolean closed = false;

        @PreDestroy
        public void close() {
            closed = true;
        }

        public void writeApplicationStatus(String message) {
            if (closed) {
                throw new IllegalStateException("Repository closed!");
            }
        }
    }

    @Singleton
    public static class Service {
        @Inject
        private Repository repository;

        @PreDestroy
        public void destroy() {
            repository.writeApplicationStatus("Closing application");
        }
    }

    @Test
    public void testDestroyOrder() {

        // This test fails because Mycila destroys singletons in the order they are bound

        CloseableInjector injector = Guice.createInjector(Stage.PRODUCTION, new Jsr250Module(), new CloseableModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(Repository.class);
                bind(Service.class);
            }
        }).getInstance(CloseableInjector.class);

        injector.close();
    }
}
