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
import com.mycila.guice.ext.jsr250.Jsr250;

import java.util.concurrent.TimeUnit;

@Jsr250Singleton
public final class RenewableSingleton extends MycilaScope {
    final long expirationDelay;

    public RenewableSingleton(long expirationDelay, TimeUnit unit) {
        this.expirationDelay = unit.toMillis(expirationDelay);
    }

    @Override
    public <T> Provider<T> scope(final Key<T> key, final Provider<T> creator) {
        return new Provider<T>() {
            private volatile T instance;
            private volatile long expirationTime;

            @Override
            public T get() {
                if (expirationTime < System.currentTimeMillis()) {
                    synchronized (this) {
                        if (expirationTime < System.currentTimeMillis()) {
                            T old = instance;
                            if (hasJSR250Module)
                                Jsr250.preDestroy(old);
                            instance = creator.get();
                            expirationTime = System.currentTimeMillis() + expirationDelay;
                        }
                    }
                }
                return instance;
            }

            @Override
            public String toString() {
                return String.format("%s[%s]", creator, RenewableSingleton.this);
            }
        };
    }
}
