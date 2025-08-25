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
package com.mycila.guice.ext.legacy;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@RunWith(JUnit4.class)
public final class LegacyProviderTest {

    static List<Integer> seq = new LinkedList<Integer>();

    @Test
    public void test_legacy() throws Exception {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Integer.class).toInstance(1);
                bind(String.class).toInstance("str");
                bind(CharSequence.class).toInstance("charSeq");

                bind(A.class).annotatedWith(Names.named("1"))
                        .toProvider(LegacyProvider.of(A.class).withConstructor(Integer.class, String.class)
                                .inject("inject", String.class)
                                .inject("inject3", CharSequence.class));

                bind(A.class).annotatedWith(Names.named("2"))
                        .toProvider(LegacyProvider.of(B.class).withFactory(B.class, "create", String.class)
                                .inject("inject", String.class)
                                .inject("inject2", CharSequence.class)
                                .inject("inject3", CharSequence.class));

                bind(A.class).annotatedWith(Names.named("3"))
                        .toProvider(LegacyProvider.of(A.class).withFactory(B.class, "create", String.class)
                                .inject("inject", String.class)
                                .inject("inject2", CharSequence.class)
                                .inject("inject3", CharSequence.class));
            }
        });

        injector.getInstance(Key.get(A.class, Names.named("1")));
        assertEquals("[10, 11, 13]", seq.toString());

        seq.clear();
        injector.getInstance(Key.get(A.class, Names.named("2")));
        assertEquals("[20, 10, 21, 22, 13]", seq.toString());

        seq.clear();
        injector.getInstance(Key.get(A.class, Names.named("3")));
        assertEquals("[20, 10, 21, 12, 13]", seq.toString());
    }

    static class A {
        A(Integer i, String a) {
            seq.add(10);
        }

        void inject(String s) {
            seq.add(11);
        }

        private void inject2(CharSequence s) {
            seq.add(12);
        }

        private void inject3(CharSequence s) {
            seq.add(13);
        }
    }

    static class B extends A {
        B(Integer i, String a) {
            super(i, a);
        }

        @Override
        void inject(String s) {
            seq.add(21);
        }

        void inject2(CharSequence s) {
            seq.add(22);
        }

        static A create(String s) {
            seq.add(20);
            return new B(1, s);
        }
    }
}
