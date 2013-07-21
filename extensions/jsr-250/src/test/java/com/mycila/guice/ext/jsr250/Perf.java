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

package com.mycila.guice.ext.jsr250;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

import javax.annotation.PostConstruct;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 * @date 2013-07-06
 */
final class Perf {

    private static long invocations;

    public static class TestClassWithPostConstruct {

        public void method1() {
        }

        public void method2() {
        }

        @PostConstruct
        public void method3() {
            invocations++;
        }

        public void method4() {
        }
    }

    public static class TestClassWithoutPostConstruct {

        public void method1() {
        }

        public void method2() {
        }

        public void method3() {
        }

        public void method4() {
        }
    }

    public static void main(String[] args) {

        int n = 1 * 1000 * 10000;

        System.out.println("To create " + n + " instances of a class with a @PostConstruct method");
        time("without Mycila", n, createSimpleInjector(), TestClassWithPostConstruct.class);
        time("with Mycila", n, createInjectorWithMycila(), TestClassWithPostConstruct.class);

        System.out.println("To create " + n + " instances of a class without @PostConstruct method");
        time("without Mycila", n, createSimpleInjector(), TestClassWithoutPostConstruct.class);
        time("with Mycila", n, createInjectorWithMycila(), TestClassWithoutPostConstruct.class);
    }

    private static void time(String name, int n, Injector injector, Class<?> clazz) {

        // warm up
        for (int i = 0; i < 1000 * 1000; i++) {
            injector.getInstance(clazz);
        }

        invocations = 0;
        long start = System.currentTimeMillis();

        for (int i = 0; i < n; i++) {
            injector.getInstance(clazz);
        }

        long end = System.currentTimeMillis();

        System.out.println("Time " + name + ": " + (end - start) + " ms     " + invocations + " invocations");
    }

    private static Injector createInjectorWithMycila() {
        return Jsr250.createInjector(Stage.PRODUCTION);
    }

    private static Injector createSimpleInjector() {
        return Guice.createInjector(Stage.PRODUCTION);
    }
}
