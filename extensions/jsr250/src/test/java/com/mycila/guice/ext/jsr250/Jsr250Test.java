/*
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

import com.google.inject.*;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import com.mycila.guice.ext.closeable.CloseableInjector;
import com.mycila.guice.ext.closeable.CloseableModule;
import com.mycila.guice.ext.injection.MBinder;
import com.mycila.guice.ext.injection.Reflect;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@RunWith(JUnit4.class)
public class Jsr250Test {

    @Test
    public void test_resource_with_type() throws Exception {
        Guice.createInjector(Stage.PRODUCTION, new Jsr250Module(), new CloseableModule()).getInstance(Res1Class.class);
        assertEquals(2, Res1Class.verified);
    }

    static class Res1Class {
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
    public void test_resource_with_name() throws Exception {
        final AA aa1 = new AA();
        final AA aa2 = new AA();
        Res2Class res2 = Guice.createInjector(Stage.PRODUCTION, new Jsr250Module(), new CloseableModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(AA.class).annotatedWith(Names.named("aa1")).toInstance(aa1);
                bind(AA.class).annotatedWith(Names.named("aa2")).toInstance(aa2);
            }
        }).getInstance(Res2Class.class);
        assertEquals(aa1, res2.aa1);
        assertEquals(aa2, res2.aa2);
        assertTrue(res2.aa1 != res2.aa2);
    }

    static class Res2Class {
        @Resource
        AA aa1;

        @Resource
        AA aa2;
    }

    @Test
    public void test_post_inject_param() throws Exception {
        assertFalse(MyM.AAA.CALLED);
        assertFalse(MyM.AAA.SECOND);
        Guice.createInjector(Stage.PRODUCTION, new Jsr250Module(), new CloseableModule(), new MyM());
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
        CloseableInjector injector = Guice.createInjector(Stage.PRODUCTION, new Jsr250Module(), new CloseableModule(), new AbstractModule() {
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
        }).getInstance(CloseableInjector.class);
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
        static final List<String> calls = new ArrayList<String>();

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
        CloseableInjector injector = Guice.createInjector(new Jsr250Module(), new CloseableModule(), new AbstractModule() {
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
        }).getInstance(CloseableInjector.class);
        B b = injector.getInstance(B.class);
        assertSame(b, injector.getInstance(B.class));
        b.intercept();
        injector.close();
        assertEquals("[1, 2, 3]", B.calls.toString());
    }

    @Test
    public void test() throws Exception {
        B.calls.clear();
        CloseableInjector injector = Guice.createInjector(Stage.PRODUCTION, new Jsr250Module(), new CloseableModule(), new AbstractModule() {
            @Override
            protected void configure() {
            }
        }).getInstance(CloseableInjector.class);
        injector.getInstance(B.class);
        assertEquals("[1, 2]", B.calls.toString());
        injector.close();
        assertEquals("[1, 2, 3]", B.calls.toString());
    }

    static class A {
        static List<Integer> calls = new LinkedList<Integer>();

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
