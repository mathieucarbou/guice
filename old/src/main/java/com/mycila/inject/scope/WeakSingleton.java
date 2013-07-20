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

import com.mycila.inject.annotation.Jsr250Singleton;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

@Jsr250Singleton
public final class WeakSingleton extends RefScope {
    @Override
    protected <T> Reference<T> build(T instance) {
        return new WeakReference<T>(instance);
    }
}
