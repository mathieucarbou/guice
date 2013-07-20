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

package com.mycila.inject.service;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.mycila.inject.util.DefaultLoader;
import com.mycila.inject.util.Loader;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class ServiceLoaderProvider<T> implements Provider<T[]> {

    @Inject
    private Injector injector;
    private Key<? extends Loader> loaderKey;
    private final Class<T> type;

    private ServiceLoaderProvider(Class<T> type) {
        this.type = type;
    }

    public ServiceLoaderProvider<T> withLoader(Class<? extends Loader> loaderType) {
        return withLoader(Key.get(loaderType));
    }

    public ServiceLoaderProvider<T> withLoader(Class<? extends Loader> loaderType, Class<? extends Annotation> annot) {
        return withLoader(Key.get(loaderType, annot));
    }

    public ServiceLoaderProvider<T> withLoader(Class<? extends Loader> loaderType, Annotation annot) {
        return withLoader(Key.get(loaderType, annot));
    }

    public ServiceLoaderProvider<T> withLoader(Key<? extends Loader> key) {
        this.loaderKey = key;
        return this;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public T[] get() {
        List<T> instances = new ArrayList<T>();
        ServiceClassLoader<T> loader = loaderKey == null ?
                ServiceClassLoader.<T>load(type, new DefaultLoader()) :
                ServiceClassLoader.<T>load(type, injector.getInstance(loaderKey));
        for (Class<T> clazz : loader) {
            if (!type.isAssignableFrom(clazz))
                throw new ClassCastException(clazz + " cannot be assigned to binded type " + type);
            instances.add(injector.getInstance(clazz));
        }
        return instances.toArray((T[]) Array.newInstance(type, instances.size()));
    }

    @Override
    public String toString() {
        return "ServiceLoaderProvider[" + type.getName() + "]";
    }

    public static <T> ServiceLoaderProvider<T> of(Class<T> type) {
        return new ServiceLoaderProvider<T>(type);
    }
}
