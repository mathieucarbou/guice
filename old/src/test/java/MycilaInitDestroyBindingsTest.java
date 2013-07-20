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

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.mycila.inject.jsr250.Jsr250;
import com.mycila.inject.jsr250.Jsr250Injector;
import org.junit.Test;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import static org.junit.Assert.assertEquals;

public class MycilaInitDestroyBindingsTest {

    public static class InitDestroyCounter {

        public int initialized;
        public int destroyed;

        @PostConstruct
        public void init() {
            initialized++;
        }

        @PreDestroy
        public void destroy() {
            destroyed++;
        }
    }

    public interface SomeInterface {
    }

    public interface AnotherInterface {
    }

    public static class NonScopedTestObject extends InitDestroyCounter implements SomeInterface, AnotherInterface {
    }

    @Singleton
    public static class TestSingleton extends InitDestroyCounter implements SomeInterface, AnotherInterface {
    }

    public static abstract class TestProvider<T> extends InitDestroyCounter implements Provider<T> {
    }

    @Test
    public void testUntargettedBinding() {
        Jsr250Injector injector = Jsr250.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(TestSingleton.class);
            }
        });

        TestSingleton component = injector.getInstance(TestSingleton.class);
        assertEquals(1, component.initialized);
        assertEquals(0, component.destroyed);

        injector.destroy();
        assertEquals(1, component.initialized);
        assertEquals(1, component.destroyed);
    }

    @Test
    public void testLinkedBinding() {
        Jsr250Injector injector = Jsr250.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(SomeInterface.class).to(TestSingleton.class);
            }
        });

        TestSingleton component = (TestSingleton) injector.getInstance(SomeInterface.class);
        assertEquals(1, component.initialized);
        assertEquals(0, component.destroyed);

        injector.destroy();
        assertEquals(1, component.initialized);
        assertEquals(1, component.destroyed);

        // Guice creates an internal jit ConstructorBinding therefor this fails as Mycila destroys on both bindings
    }

    @Test
    public void testLinkedAndUntargettedBinding() {
        Jsr250Injector injector = Jsr250.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(TestSingleton.class);
                bind(SomeInterface.class).to(TestSingleton.class);
            }
        });

        TestSingleton component = (TestSingleton) injector.getInstance(SomeInterface.class);
        assertEquals(1, component.initialized);
        assertEquals(0, component.destroyed);

        injector.destroy();
        assertEquals(1, component.initialized);
        assertEquals(1, component.destroyed);

        // Both bindings return the same object therefor this fails as Mycila destroys on both bindings
    }

    @Test
    public void testInstanceBinding() {
        Jsr250Injector injector = Jsr250.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                TestSingleton instance = new TestSingleton();
                bind(SomeInterface.class).toInstance(instance);
                bind(AnotherInterface.class).toInstance(instance);
            }
        });

        TestSingleton component = (TestSingleton) injector.getInstance(SomeInterface.class);
        assertEquals(1, component.initialized);
        assertEquals(0, component.destroyed);

        injector.destroy();
        assertEquals(1, component.initialized);
        assertEquals(1, component.destroyed);

        // Both bindings return the same object therefor this fails as Mycila destroys on both bindings
    }

    @Test
    public void testNonSingletonInstanceBinding() {
        Jsr250Injector injector = Jsr250.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                NonScopedTestObject instance = new NonScopedTestObject();
                bind(SomeInterface.class).toInstance(instance);
                bind(AnotherInterface.class).toInstance(instance);
            }
        });

        // The object is not scoped but Guice considers instance bindings to be singletons so Mycila will destroy it

        NonScopedTestObject component = (NonScopedTestObject) injector.getInstance(SomeInterface.class);
        assertEquals(1, component.initialized);
        assertEquals(0, component.destroyed);

        injector.destroy();
        assertEquals(1, component.initialized);
        assertEquals(1, component.destroyed);

        // Both bindings return the same object therefor this fails as Mycila destroys on both bindings
    }

    @Test
    public void testProviderBinding() {
        Jsr250Injector injector = Jsr250.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                final TestSingleton instance = new TestSingleton();
                bind(TestSingleton.class).toProvider(new Provider<TestSingleton>() {
                    @Override
                    public TestSingleton get() {
                        return instance;
                    }
                });
                bind(SomeInterface.class).toProvider(new Provider<SomeInterface>() {
                    @Override
                    public TestSingleton get() {
                        return instance;
                    }
                });
            }
        });

        // Provider bindings are not scoped so Mycila will not destroy them

        TestSingleton component = (TestSingleton) injector.getInstance(SomeInterface.class);
        assertEquals(0, component.initialized);
        assertEquals(0, component.destroyed);

        injector.destroy();
        assertEquals(0, component.initialized);
        assertEquals(0, component.destroyed);
    }

    @Test
    public void testProvider() {

        final TestProvider<TestSingleton> testProvider1 = new TestProvider<TestSingleton>() {
            @Override
            public TestSingleton get() {
                return new TestSingleton();
            }
        };
        final TestProvider<SomeInterface> testProvider2 = new TestProvider<SomeInterface>() {
            @Override
            public TestSingleton get() {
                return new TestSingleton();
            }
        };

        Jsr250Injector injector = Jsr250.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(TestSingleton.class).toProvider(testProvider1);
                bind(SomeInterface.class).toProvider(testProvider2);
            }
        });

        // Guice will do injection on the provider but will not destroy it since its not singleton scoped

        assertEquals(1, testProvider1.initialized);
        assertEquals(0, testProvider1.destroyed);
        assertEquals(1, testProvider2.initialized);
        assertEquals(0, testProvider2.destroyed);

        injector.destroy();

        assertEquals(1, testProvider1.initialized);
        assertEquals(1, testProvider1.destroyed);
        assertEquals(1, testProvider2.initialized);
        assertEquals(1, testProvider2.destroyed);
    }
}
