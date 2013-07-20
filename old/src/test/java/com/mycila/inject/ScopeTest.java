/**
 * Copyright (C) 2010 Mycila <mathieu.carbou@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mycila.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.util.Providers;
import com.mycila.inject.annotation.RenewableSingleton;
import com.mycila.inject.scope.ExpiringSingleton;
import com.mycila.inject.scope.ResetScope;
import com.mycila.inject.scope.ResetSingleton;
import com.mycila.inject.scope.WeakSingleton;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.inject.Provider;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mycila.inject.MycilaGuice.in;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@RunWith(JUnit4.class)
public final class ScopeTest {

    @Test
    public void test_reset_singleton() throws Exception {
        final AtomicInteger counter = new AtomicInteger();
        Provider<Object> unscoped = mock(Provider.class);
        when(unscoped.get()).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                counter.incrementAndGet();
                return new Object();
            }
        });
        ResetScope scope = new ResetSingleton();
        Provider<Object> provider = scope.scope(Key.get(Object.class), Providers.guicify(unscoped));
        assertEquals(0, counter.get());
        provider.get();
        assertEquals(1, counter.get());
        provider.get();
        assertEquals(1, counter.get());
        scope.reset();
        provider.get();
        assertEquals(2, counter.get());
        provider.get();
        assertEquals(2, counter.get());
    }

    @Test
    public void test_expire() throws Exception {
        Provider<Object> unscoped = mock(Provider.class);
        when(unscoped.get()).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return new Object();
            }
        });
        Provider<Object> provider = new ExpiringSingleton(500, TimeUnit.MILLISECONDS).scope(Key.get(Object.class), Providers.guicify(unscoped));

        Object o = provider.get();
        assertNotNull(o);
        assertSame(o, provider.get());

        Thread.sleep(600);

        assertNull(provider.get());
    }

    @Test
    public void test_weak() throws Exception {
        Provider<Object> unscoped = mock(Provider.class);
        when(unscoped.get()).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return new Object();
            }
        });

        Provider<Object> provider = new WeakSingleton().scope(null, Providers.guicify(unscoped));

        Object o = provider.get();
        int hash = o.hashCode();
        assertNotNull(o);

        System.gc();
        System.gc();

        assertSame(o, provider.get());

        o = provider.get();
        System.gc();
        System.gc();

        assertNotSame(hash, o.hashCode());
    }

    @Test
    public void test_renewable() throws Exception {
        Provider<Object> unscoped = mock(Provider.class);
        when(unscoped.get()).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return new Object();
            }
        });
        Provider<Object> provider = new com.mycila.inject.scope.RenewableSingleton(500, TimeUnit.MILLISECONDS).scope(Key.get(Object.class), Providers.guicify(unscoped));

        Object o = provider.get();
        assertNotNull(o);
        assertSame(o, provider.get());

        Thread.sleep(600);

        assertNotSame(o, provider.get());
    }

    @Test
    public void test_with_injector() throws Exception {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bindScope(RenewableSingleton.class, in(binder()).renewableSingleton(500, TimeUnit.MILLISECONDS));
                bind(Object.class).toProvider(Providers.guicify(new Provider<Object>() {
                    @Override
                    public Object get() {
                        return new Object();
                    }
                })).in(RenewableSingleton.class);
            }
        });

        Object o1 = injector.getInstance(Key.get(Object.class));
        assertNotNull(o1);
        Object o2 = injector.getInstance(Key.get(Object.class));
        assertNotNull(o2);
        assertSame(o1, o2);
        Thread.sleep(600);
        assertNotSame(o1, injector.getInstance(Key.get(Object.class)));
    }

}