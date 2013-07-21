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
package com.mycila.inject.internal;

import java.lang.reflect.Method;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
final class Signature {
    public final Class[] parameterTypes;
    private final int hash;
    public final Method method;

    public Signature(Method method) {
        this.method = method;
        this.parameterTypes = method.getParameterTypes();
        int h = method.hashCode();
        h = h * 31 + parameterTypes.length;
        for (Class parameterType : parameterTypes) {
            h = h * 31 + parameterType.hashCode();
        }
        this.hash = h;
    }

    @Override
    public int hashCode() {
        return this.hash;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Signature)) {
            return false;
        }
        Signature other = (Signature) o;
        if (!method.getName().equals(other.method.getName())) {
            return false;
        }
        if (parameterTypes.length != other.parameterTypes.length) {
            return false;
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i] != other.parameterTypes[i]) {
                return false;
            }
        }
        return true;
    }
}
