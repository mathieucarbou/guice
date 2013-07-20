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

package com.mycila.inject.scope;

import com.google.inject.Key;
import com.google.inject.Provider;

import java.lang.ref.Reference;

abstract class RefScope extends MycilaScope {

    static final Object NULL = new Object();

    @Override
    public final <T> Provider<T> scope(Key<T> key, final Provider<T> creator) {
        return new Provider<T>() {
            private volatile Reference<T> ref;

            @Override
            public T get() {
                return ref == null ? newRef(creator) : fromRef(creator);
            }

            private T fromRef(Provider<T> unscoped) {
                T instance = ref.get();
                if (instance == NULL) return null;
                if (instance != null) return instance;
                return newRef(unscoped);
            }

            private T newRef(Provider<T> unscoped) {
                T instance = unscoped.get();
                ref = (Reference<T>) build(instance == null ? NULL : instance);
                return instance;
            }

            @Override
            public String toString() {
                return String.format("%s[%s]", creator, RefScope.this);
            }
        };
    }

    protected abstract <T> Reference<T> build(T instance);
}
