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
package com.mycila.inject.service;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.util.Types;
import com.mycila.inject.util.Loader;

import java.lang.annotation.Annotation;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class ServiceLoaderModule<T> implements Module {

    private final Key<T[]> listKey;
    private Key<? extends Loader> loaderKey;

    private ServiceLoaderModule(Key<T[]> listKey) {
        this.listKey = listKey;
    }

    public ServiceLoaderModule<T> withLoader(Class<? extends Loader> loaderType) {
        return withLoader(Key.get(loaderType));
    }

    public ServiceLoaderModule<T> withLoader(Class<? extends Loader> loaderType, Class<? extends Annotation> annot) {
        return withLoader(Key.get(loaderType, annot));
    }

    public ServiceLoaderModule<T> withLoader(Class<? extends Loader> loaderType, Annotation annot) {
        return withLoader(Key.get(loaderType, annot));
    }

    public ServiceLoaderModule<T> withLoader(Key<? extends Loader> key) {
        this.loaderKey = key;
        return this;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(listKey).toProvider(ServiceLoaderProvider.of(getType()).withLoader(loaderKey));
    }

    @SuppressWarnings({"unchecked"})
    private Class<T> getType() {
        return (Class<T>) listKey.getTypeLiteral().getRawType().getComponentType();
    }

    @SuppressWarnings({"unchecked"})
    public static <T> Module of(Class<T> serviceClass) {
        return new ServiceLoaderModule<T>((Key<T[]>) Key.get(Types.arrayOf(serviceClass)));
    }

    @SuppressWarnings({"unchecked"})
    public static <T> Module of(Class<T> serviceClass, Annotation annotation) {
        return new ServiceLoaderModule<T>((Key<T[]>) Key.get(Types.arrayOf(serviceClass), annotation));
    }

    @SuppressWarnings({"unchecked"})
    public static <T> Module of(Class<T> serviceClass, Class<? extends Annotation> annotationType) {
        return new ServiceLoaderModule<T>((Key<T[]>) Key.get(Types.arrayOf(serviceClass), annotationType));
    }

}
