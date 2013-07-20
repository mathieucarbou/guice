/**
 * Copyright (C) 2010 mycila.com <mathieu.carbou@gmail.com>
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

package com.mycila.inject.internal;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
final class WeakCache<K, V> {

    private static final Object NULL = new Object();

    private final WeakHashMap<K, WeakReference<V>> cache = new WeakHashMap<K, WeakReference<V>>();
    private final Provider<K, V> provider;

    public WeakCache(Provider<K, V> provider) {
        this.provider = provider;
    }

    public V get(K key) {
        WeakReference<V> ref = cache.get(key);
        V val = null;
        if (ref != null)
            val = ref.get();
        if (val == null) {
            val = provider.get(key);
            if (val == null)
                val = (V) NULL;
            cache.put(key, new WeakReference<V>(val));
        }
        return val == NULL ? null : val;
    }

    public static interface Provider<K, V> {
        V get(K key);
    }

}
