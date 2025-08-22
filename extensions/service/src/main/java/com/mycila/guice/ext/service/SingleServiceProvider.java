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
package com.mycila.guice.ext.service;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;

import java.util.NoSuchElementException;
import java.util.ServiceLoader;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public class SingleServiceProvider<T> implements Provider<T> {

    @Inject
    private Injector injector;
    private final Class<T> type;
    private final ClassLoader classLoader;
    private boolean failIfNotFound = true;

    public SingleServiceProvider(Class<T> type) {
        this(type, type.getClassLoader());
    }

    public SingleServiceProvider(Class<T> type, ClassLoader classLoader) {
        this.type = type;
        this.classLoader = classLoader;
    }

    public SingleServiceProvider<T> allowMissingImplementation() {
        failIfNotFound = false;
        return this;
    }

    @Override
    public T get() {
        try {
            T instance = ServiceLoader.load(type, classLoader).iterator().next();
            injector.injectMembers(instance);
            return instance;
        } catch (NoSuchElementException e) {
            if (failIfNotFound) {
                throw new ProvisionException("No implementation found in classpath for interface: " + type.getName());
            } else {
                return null;
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type.getName() + "]";
    }

}
