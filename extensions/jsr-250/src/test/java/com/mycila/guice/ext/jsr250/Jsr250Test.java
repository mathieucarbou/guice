/**
 * Copyright (C) 2010 Mycila (mathieu.carbou@gmail.com)
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
package com.mycila.guice.ext.jsr250;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.google.inject.matcher.Matchers;
import com.mycila.guice.ext.injection.MBinder;
import com.mycila.guice.ext.injection.Reflect;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@RunWith(JUnit4.class)
public final class Jsr250Test {

    @Test
    public void test_resource() throws Exception {
        Jsr250.createInjector(Stage.PRODUCTION).getInstance(ResClass.class);
        assertEquals(2, ResClass.verified);
    }

    static class ResClass {
        static int verified;
        @Resource
        Injector injector;

        @Resource
        Provider<Injector> provider;

        @Resource
        void init(AA aa) {
            // field injection is done before method injection
            assertNotNull(injector);
            assertNotNull(provider);
            assertNotNull(aa);
            verified++;
        }

        @PostConstruct
        void init() {
            assertNotNull(injector);
            assertNotNull(provider);
            assertSame(injector, provider.get());
            verified++;
        }
    }

    @Test
    public void test_post_inject_param() throws Exception {
        assertFalse(MyM.AAA.CALLED);
        assertFalse(MyM.AAA.SECOND);
        Jsr250.createInjector(Stage.PRODUCTION, new MyM());
        assertTrue(MyM.AAA.CALLED);
        assertTrue(MyM.AAA.SECOND);
    }

    static class MyM extends AbstractModule {
        @Override
        protected void configure() {
            bind(AAA.class);
        }

        @Singleton
        static class AAA {
            static boolean CALLED;
            static boolean SECOND;

            @Inject
            BBB bbb;

            @PostConstruct
            void init() {
                SECOND = true;
                assertNotNull(bbb);
            }

            @PostConstruct
            void init(BBB bb) {
                assertNotNull(bb);
                assertNotNull(bbb);
                CALLED = true;
            }
        }
    }

    @Singleton
    static class BBB {
    }

    @Test
    public void test_destroy() throws Exception {
        final Class[] cc = {AA.class};
        Jsr250Injector injector = Jsr250.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure() {
                for (Class<?> c : cc) {
                    bind(c);
                }
                // just for fun
                MBinder.wrap(binder()).bindInterceptor(Matchers.subclassesOf(Base.class), Matchers.any(), new MethodInterceptor() {
                    @Override
                    public Object invoke(MethodInvocation invocation) throws Throwable {
                        System.out.println("intercept: " + Reflect.getTargetClass(invocation.getThis()).getSimpleName() + "." + invocation.getMethod().getName());
                        return invocation.proceed();
                    }
                });
            }
        });
        for (Class<?> c : cc) {
            injector.getInstance(c);
            injector.getInstance(c);
        }

        Collections.sort(Base.calls);
        assertEquals("[]", Base.calls.toString());

        for (Class<?> c : cc) {
            injector.getInstance(c);
        }

        injector.close();

        Collections.sort(Base.calls);
        assertEquals("[AA]", Base.calls.toString());
    }

    static class Base {
        static final List<String> calls = new ArrayList<>();

        @PreDestroy
        void close() {
            calls.add(Reflect.getTargetClass(getClass()).getSimpleName());
        }
    }

    @Singleton
    static class AA extends Base {
    }

    @Test
    public void test_inject_in_interceptor() throws Exception {
        B.calls.clear();
        Jsr250Injector injector = Jsr250.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                MBinder.wrap(binder()).bindInterceptor(Matchers.subclassesOf(A.class), Matchers.any(), new MethodInterceptor() {
                    @Resource
                    Injector injector;

                    @Override
                    public Object invoke(MethodInvocation invocation) throws Throwable {
                        System.out.println("intercept: " + invocation.getMethod());
                        assertNotNull(injector);
                        return invocation.proceed();
                    }
                });
            }
        });
        B b = injector.getInstance(B.class);
        assertSame(b, injector.getInstance(B.class));
        b.intercept();
        injector.close();
        assertEquals("[1, 2, 3]", B.calls.toString());
    }

    @Test
    public void test() throws Exception {
        B.calls.clear();
        Jsr250Injector injector = Jsr250.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure() {
            }
        });
        injector.getInstance(B.class);
        assertEquals("[1, 2]", B.calls.toString());
        injector.close();
        assertEquals("[1, 2, 3]", B.calls.toString());
    }

    static class A {
        static List<Integer> calls = new LinkedList<>();

        @Inject
        void method(B b) {
            calls.add(1);
        }

        @PostConstruct
        void init() {
            calls.add(2);
        }

        @PreDestroy
        void close() {
            calls.add(3);
        }
    }

    @Singleton
    static class B extends A {
        void intercept() {
        }
    }

}
