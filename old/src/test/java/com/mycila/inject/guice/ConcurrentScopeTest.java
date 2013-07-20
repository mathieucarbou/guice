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

package com.mycila.inject.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Stage;
import com.mycila.inject.annotation.ConcurrentSingleton;
import com.mycila.guice.ext.jsr250.Jsr250;
import com.mycila.guice.ext.jsr250.Jsr250Injector;
import com.mycila.inject.scope.ExtraScopeModule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@RunWith(JUnit4.class)
public final class ConcurrentScopeTest {

    @Test
    public void test_concurrent() throws Exception {
        long start = System.currentTimeMillis();
        Jsr250Injector injector = Jsr250.createInjector(Stage.DEVELOPMENT, new ExtraScopeModule(), new AbstractModule() {
            public void configure() {
                bind(C.class);
                bind(D.class);
            }
        });
        injector.getInstance(A.class);
        long elapsed = System.currentTimeMillis() - start;
        injector.destroy();
        System.out.printf("Completed in %d ms%n", elapsed);
        assertTrue(elapsed < 5000);
    }

    @ConcurrentSingleton
    static class A {
        @Inject
        public A(Provider<B> b, Provider<C> c) {
            try {
                System.out.printf("Starting A on thread %s%n", Thread.currentThread().getName());
                TimeUnit.SECONDS.sleep(1);
                System.out.printf("A getting B and C instances on thread %s%n", Thread.currentThread().getName());
                b.get();
                c.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                System.out.println("Started A");
            }
        }
    }

    @ConcurrentSingleton
    static class B {
        @Inject
        public B() {
            try {
                System.out.printf("Starting B on thread %s%n", Thread.currentThread().getName());
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                System.out.println("Started B");
            }
        }
    }

    @ConcurrentSingleton
    static class C {
        @Inject
        public C(Provider<D> d) {
            try {
                System.out.printf("Starting C on thread %s%n", Thread.currentThread().getName());
                TimeUnit.SECONDS.sleep(3);
                System.out.printf("C getting D instance on thread %s%n", Thread.currentThread().getName());
                d.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                System.out.println("Started C");
            }
        }
    }

    @ConcurrentSingleton
    static class D {
        @Inject
        public D() {
            try {
                System.out.printf("Starting D on thread %s%n", Thread.currentThread().getName());
                TimeUnit.SECONDS.sleep(4);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                System.out.println("Started D");
            }
        }
    }

    @ConcurrentSingleton
    static class Solo {
        @Inject
        public Solo() {
            try {
                System.out.printf("Starting Solo on thread %s%n", Thread.currentThread().getName());
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                System.out.println("Started Solo");
            }
        }
    }

}