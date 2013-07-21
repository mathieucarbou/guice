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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public class Proxy {

    private Proxy() {
    }

    private static final WeakCache<Method, MethodInvoker> INVOKER_CACHE = new WeakCache<Method, MethodInvoker>(new WeakCache.Provider<Method, MethodInvoker>() {
        @Override
        public MethodInvoker get(final Method method) {
            int modifiers = method.getModifiers();
            if (!Modifier.isPrivate(modifiers) && !Modifier.isProtected(modifiers)) {
                try {
                    final net.sf.cglib.reflect.FastMethod fastMethod = BytecodeGen.newFastClass(method.getDeclaringClass(), BytecodeGen.Visibility.forMember(method)).getMethod(method);
                    return new MethodInvoker() {
                        public Object invoke(Object target, Object... parameters) throws IllegalAccessException, InvocationTargetException {
                            return fastMethod.invoke(target, parameters);
                        }
                    };
                } catch (net.sf.cglib.core.CodeGenerationException e) {/* fall-through */}
            }
            if (!Modifier.isPublic(modifiers) || !Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
                method.setAccessible(true);
            }
            return new MethodInvoker() {
                public Object invoke(Object target, Object... parameters) throws IllegalAccessException, InvocationTargetException {
                    return method.invoke(target, parameters);
                }
            };
        }
    });

    public static MethodInvoker invoker(final Method method) {
        return INVOKER_CACHE.get(method);
    }

}
