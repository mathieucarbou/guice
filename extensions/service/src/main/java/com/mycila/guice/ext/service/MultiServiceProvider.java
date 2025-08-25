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

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

import java.lang.reflect.Array;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public class MultiServiceProvider<T> implements Provider<T[]> {

    @Inject
    private Injector injector;
    private final Class<T> type;
    private final ClassLoader classLoader;

    public MultiServiceProvider(Class<T> type) {
        this(type, type.getClassLoader());
    }

    public MultiServiceProvider(Class<T> type, ClassLoader classLoader) {
        this.type = type;
        this.classLoader = classLoader;
    }

    @Override
    public T[] get() {
        List<T> instances = new LinkedList<T>();
        for (T instance : ServiceLoader.load(type, classLoader)) {
            injector.injectMembers(instance);
            instances.add(instance);
        }
        @SuppressWarnings("unchecked")
        T[] array = (T[]) Array.newInstance(type, instances.size());
        return instances.toArray(array);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type.getName() + "]";
    }

}
