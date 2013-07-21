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

import com.google.inject.AbstractModule;
import org.junit.Test;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MycilaJsr250Test {

    public static abstract class LifecycleBase {
        public String startSequence = "";
        public String stopSequence = "";
    }

    @Singleton
    public static class LifecycleSimple extends LifecycleBase {
        @PostConstruct
        public void init() {
            startSequence += "A";
        }

        @PreDestroy
        public void destroy() {
            stopSequence += "A";
        }
    }

    @Singleton
    public static class LifecycleMultiple extends LifecycleBase {
        @PostConstruct
        public void init() {
            startSequence += "A";
        }

        @PostConstruct
        public void init2() {
            startSequence += "B";
        }

        @PreDestroy
        public void destroy() {
            stopSequence += "A";
        }

        @PreDestroy
        public void destroy2() {
            stopSequence += "B";
        }
    }

    @Singleton
    public static class LifecycleExtends extends LifecycleSimple {
        @PostConstruct
        public void init2() {
            startSequence += "X";
        }

        @PreDestroy
        public void destroy2() {
            stopSequence += "X";
        }
    }

    @Singleton
    public static class LifecycleOverrides extends LifecycleSimple {
        @Override
        @PostConstruct
        public void init() {
            startSequence += "B";
        }

        @Override
        @PreDestroy
        public void destroy() {
            stopSequence += "B";
        }
    }

    @Singleton
    public static class LifecycleOverridesRemovesAnnotations extends LifecycleOverrides {
        @Override
        public void init() {
            startSequence += "C";
        }

        @Override
        public void destroy() {
            stopSequence += "C";
        }
    }

    @Singleton
    public static class LifecyclePrivateMethods extends LifecycleBase {
        @PostConstruct
        private void init() {
            startSequence += "D";
        }

        @PreDestroy
        private void destroy() {
            stopSequence += "D";
        }
    }

    @Singleton
    public static class LifecycleSameNamePrivateMethods extends LifecyclePrivateMethods {
        @PostConstruct
        private void init() {
            startSequence += "E";
        }

        @PreDestroy
        private void destroy() {
            stopSequence += "E";
        }
    }

    @Test
    public void testLifecycle() {
        assertLifeCycleSequence(LifecycleSimple.class, "A", "A");
        assertLifeCycleSequence(LifecycleExtends.class, "AX", "XA");
        assertLifeCycleSequence(LifecycleOverrides.class, "B", "B");
        assertLifeCycleSequence(LifecycleOverridesRemovesAnnotations.class, "", "");
        assertLifeCycleSequence(LifecyclePrivateMethods.class, "D", "D");
        //TODO: verify if private methodes of super classes should be invoked before subclasses in case of @PostConstruct
        //assertLifeCycleSequence(LifecycleSameNamePrivateMethods.class, "ED", "DE");
        assertLifeCycleSequence(LifecycleSameNamePrivateMethods.class, "DE", "ED");

        // order among methods in same class is undefined so we just test that all of them were called
        assertLifeCycleSequenceContainsAll(LifecycleMultiple.class, "AB", "BA");
    }

    private void assertLifeCycleSequenceContainsAll(final Class<? extends LifecycleBase> clazz, String startSequence, String endSequence) {
        Jsr250Injector injector = createInjector(clazz);
        LifecycleBase component = injector.getInstance(clazz);
        assertContainsAll(component.startSequence, startSequence);
        injector.close();
        assertContainsAll(component.stopSequence, endSequence);
    }

    private void assertLifeCycleSequence(final Class<? extends LifecycleBase> clazz, String expectedStartSequence, String expectedStopSequence) {
        Jsr250Injector injector = createInjector(clazz);
        LifecycleBase component = injector.getInstance(clazz);
        assertEquals(expectedStartSequence, component.startSequence);
        injector.close();
        assertEquals(expectedStopSequence, component.stopSequence);
    }

    private Jsr250Injector createInjector(final Class<? extends LifecycleBase> clazz) {
        return Jsr250.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(clazz);
            }
        });
    }

    private void assertContainsAll(String string, String requiredCharacters) {
        for (char ch : requiredCharacters.toCharArray()) {
            if (string.indexOf(ch) == -1) {
                fail("String [" + string + "] does not contain character [" + ch + "]");
            }
        }
    }
}

