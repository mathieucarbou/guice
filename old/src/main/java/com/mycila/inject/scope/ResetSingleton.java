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
import com.mycila.inject.annotation.Jsr250Singleton;
import com.mycila.inject.jsr250.Jsr250;

import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Jsr250Singleton
public final class ResetSingleton extends MycilaScope implements ResetScope {

    private static final Object NULL = new Object();

    private final Map<Key<?>, Object> singletons = new HashMap<Key<?>, Object>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock r = lock.readLock();
    private final Lock w = lock.writeLock();

    @Override
    public void reset() {
        Map<Key<?>, Object> map = new HashMap<Key<?>, Object>();
        try {
            w.lock();
            if (hasJSR250Module) {
                map.putAll(singletons);
            }
            singletons.clear();
        } finally {
            w.unlock();
        }
        for (Map.Entry<Key<?>, Object> entry : map.entrySet())
            Jsr250.preDestroy(entry.getValue());
    }

    @PreDestroy
    public void shutdown() {
        singletons.clear();
    }

    @Override
    public <T> Provider<T> scope(final Key<T> key, final Provider<T> creator) {
        return new Provider<T>() {
            public T get() {
                Object t;
                try {
                    r.lock();
                    t = singletons.get(key);
                } finally {
                    r.unlock();
                }
                if (t == null) {
                    try {
                        w.lock();
                        t = creator.get();
                        t = t == null ? NULL : t;
                        singletons.put(key, t);
                    } finally {
                        w.unlock();
                    }
                }
                return t == NULL ? null : (T) t;
            }

            @Override
            public String toString() {
                return String.format("%s[%s]", creator, ResetSingleton.this);
            }
        };
    }
}
