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
package com.mycila.guice.ext.service;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@RunWith(JUnit4.class)
public class ServiceProviderTest {

    @Test
    public void test_single() throws Exception {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Serv.class).toProvider(new SingleServiceProvider<Serv>(Serv.class));
            }
        });
        assertEquals(ServA.class, injector.getInstance(Serv.class).getClass());
    }

    @Test
    public void test_single_not_found() throws Exception {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Serv2.class).toProvider(new SingleServiceProvider<Serv2>(Serv2.class).allowMissingImplementation());
            }
        });
        assertNull(injector.getInstance(Serv2.class));
    }

    @Test
    public void test_multi() throws Exception {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Serv[].class).toProvider(new MultiServiceProvider<Serv>(Serv.class));
            }
        });
        Serv[] array = injector.getInstance(Serv[].class);
        assertEquals(2, array.length);
        assertEquals(ServA.class, array[0].getClass());
        assertEquals(ServB.class, array[1].getClass());
    }

    @Test
    public void test_multi_not_found() throws Exception {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Serv2[].class).toProvider(new MultiServiceProvider<Serv2>(Serv2.class));
            }
        });
        Serv2[] array = injector.getInstance(Serv2[].class);
        assertEquals(0, array.length);
    }

}
