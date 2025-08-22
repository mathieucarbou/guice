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
package com.mycila.guice.ext.legacy;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.mycila.guice.ext.injection.MethodInvoker;
import com.mycila.guice.ext.injection.MycilaGuiceException;
import com.mycila.guice.ext.injection.Reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;

import static com.google.common.collect.Iterators.filter;

public abstract class LegacyProvider<T> implements Provider<T> {

    @Inject
    Injector fromGuice;
    final Key<?>[] parametersTypes;
    final Class<T> providedType;

    private LegacyProvider(Class<T> providedType, Key<?>... parametersTypes) {
        this.parametersTypes = parametersTypes;
        this.providedType = providedType;
    }

    public LegacyProvider<T> inject(String methodName, Class<?>... paramTypes) {
        return inject(methodName, toKeys(paramTypes));
    }

    public LegacyProvider<T> inject(String methodName, Key<?>... paramKeys) {
        return new MethodLegacyProvider<T>(this, providedType, methodName, paramKeys);
    }

    final Object[] getParameterValues(Injector injector) {
        Object[] values = new Object[parametersTypes.length];
        int i = 0;
        for (Key<?> key : parametersTypes)
            values[i++] = injector.getInstance(key);
        return values;
    }

    @Override
    public final T get() {
        return get(fromGuice);
    }

    abstract T get(Injector injector);

    @Override
    public String toString() {
        return "LegacyProvider[" + providedType.getName() + "]";
    }

    /* static ctors */

    public static <T> ConstructBuilder<T> of(Class<T> type) {
        return new ConstructBuilder<T>(type);
    }

    private static Class<?>[] toClasses(Key<?>... keys) {
        Class<?>[] types = new Class[keys.length];
        int i = 0;
        for (Key<?> key : keys)
            types[i++] = (Class<?>) key.getTypeLiteral().getType();
        return types;
    }

    private static Key<?>[] toKeys(Class<?>... classes) {
        Key<?>[] types = new Key[classes.length];
        int i = 0;
        for (Class<?> c : classes)
            types[i++] = Key.get(c);
        return types;
    }

    private static Method findMethod(Class<?> declaring, String methodName, Key<?>... paramTypes) {
        try {
            return declaring.getMethod(methodName, toClasses(paramTypes));
        } catch (NoSuchMethodException e) {
            Iterator<Method> methods = filter(Reflect.findAllMethods(declaring).iterator(), Reflect.withSignature(methodName, toClasses(paramTypes)));
            if (!methods.hasNext())
                throw new MycilaGuiceException("Unable to find method " + methodName + " in class " + declaring.getName() + " matching given parameter types", e);
            return methods.next();
        }
    }

    public static class ConstructBuilder<T> {
        private final Class<T> type;

        private ConstructBuilder(Class<T> type) {
            this.type = type;
        }

        public LegacyProvider<T> withDefaultConstructor() {
            return withConstructor(new Key<?>[0]);
        }

        public LegacyProvider<T> withConstructor(Class<?>... paramTypes) {
            return withConstructor(toKeys(paramTypes));
        }

        public LegacyProvider<T> withConstructor(Key<?>... paramKeys) {
            return new ConstructorLegacyProvider<T>(type, paramKeys);
        }

        public LegacyProvider<T> withFactory(Class<?> factoryType, String methodName, Class<?>... paramTypes) {
            return withFactory(Key.get(factoryType), methodName, toKeys(paramTypes));
        }

        public LegacyProvider<T> withFactory(Key<?> factoryType, String methodName, Key<?>... paramTypes) {
            return new FactoryLegacyProvider<T>(type, factoryType, methodName, paramTypes);
        }
    }

    private static class ConstructorLegacyProvider<T> extends LegacyProvider<T> {
        private final Constructor<T> constructor;

        ConstructorLegacyProvider(Class<T> type, Key<?>... parametersTypes) {
            super(type, parametersTypes);
            try {
                this.constructor = type.getDeclaredConstructor(toClasses(parametersTypes));
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }

        @Override
        public T get(Injector injector) {
            try {
                if (!constructor.isAccessible())
                    constructor.setAccessible(true);
                return constructor.newInstance(getParameterValues(injector));
            } catch (Exception e) {
                throw MycilaGuiceException.toRuntime(e);
            }
        }
    }

    private static class FactoryLegacyProvider<T> extends LegacyProvider<T> {
        private final Method method;
        private final Key<?> factoryKey;

        FactoryLegacyProvider(Class<T> type, Key<?> factoryKey, String methodName, Key<?>... paramTypes) {
            super(type, paramTypes);
            this.factoryKey = factoryKey;
            this.method = findMethod(factoryKey.getTypeLiteral().getRawType(), methodName, paramTypes);
        }

        @Override
        public T get(Injector injector) {
            Object factory = Modifier.isStatic(method.getModifiers()) ? null : injector.getInstance(factoryKey);
            try {
                return providedType.cast(MethodInvoker.on(method).invoke(factory, getParameterValues(injector)));
            } catch (Exception e) {
                throw MycilaGuiceException.toRuntime(e);
            }
        }
    }

    private static class MethodLegacyProvider<T> extends LegacyProvider<T> {
        private final LegacyProvider<T> instance;
        private final Method method;

        MethodLegacyProvider(LegacyProvider<T> instance, Class<T> providedType, String methodName, Key<?>... paramTypes) {
            super(providedType, paramTypes);
            this.instance = instance;
            this.method = findMethod(providedType, methodName, paramTypes);
        }

        @Override
        public T get(Injector injector) {
            T target = instance.get(injector);
            try {
                MethodInvoker.on(method).invoke(target, getParameterValues(injector));
                return target;
            } catch (Exception e) {
                throw MycilaGuiceException.toRuntime(e);
            }
        }
    }

}
