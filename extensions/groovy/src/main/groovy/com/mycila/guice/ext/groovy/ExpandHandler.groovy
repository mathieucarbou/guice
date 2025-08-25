/*
 * Copyright (C) 2010-2025 Mycila (mathieu.carbou@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mycila.guice.ext.groovy

import com.google.inject.TypeLiteral
import com.mycila.guice.ext.injection.MethodHandler

import java.lang.reflect.Method

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
class ExpandHandler implements MethodHandler<Expand> {
    @Override
    void handle(TypeLiteral<?> type, Object instance, Method method, Expand annotation) {
        Class<?> modelType = annotation.value()
        String name = annotation.name() ?: method.name
        Class<?>[] paramTypes = method.parameterTypes
        MetaMethod mm = instance.metaClass.pickMethod(method.name, paramTypes)
        if (paramTypes && paramTypes[0] == modelType) {
            modelType.metaClass[name] << { Object[] args ->
                Object[] params = new Object[args.length + 1]
                params[0] = delegate
                if (args.length) {
                    System.arraycopy(args, 0, params, 1, args.length)
                }
                def pTypes = mm.parameterTypes
                if (params.size() != pTypes.length) {
                    throw new IllegalArgumentException('Bad argument size')
                }
                for (int i = 0; i < pTypes.length; i++) {
                    if (params[i] != null && !pTypes[i].isDirectlyAssignable(params[i])) {
                        params[i] = params[i].asType(pTypes[i].theClass)
                    }
                }
                return mm.doMethodInvoke(instance, params)
            }
        } else {
            modelType.metaClass.static[name] << { Object[] args ->
                Object[] params = args && args[0] != null ? (Object[]) args[0] : args
                def pTypes = mm.parameterTypes
                if (params.length != pTypes.length) {
                    throw new IllegalArgumentException('Bad argument size')
                }
                for (int i = 0; i < pTypes.length; i++) {
                    if (params[i] != null && !pTypes[i].isDirectlyAssignable(params[i])) {
                        params[i] = params[i].asType(pTypes[i].theClass)
                    }
                }
                return mm.doMethodInvoke(instance, params)
            }
        }
    }
}
