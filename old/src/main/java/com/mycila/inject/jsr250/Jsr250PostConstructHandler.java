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

package com.mycila.inject.jsr250;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.mycila.inject.MycilaGuiceException;
import com.mycila.inject.injector.MethodHandlerSkeleton;
import com.mycila.inject.internal.Proxy;
import com.mycila.inject.internal.Reflect;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
final class Jsr250PostConstructHandler extends MethodHandlerSkeleton<PostConstruct> {

    @Inject
    Provider<Injector> injector;

    @Override
    public <T> void handle(TypeLiteral<? extends T> type, T instance, Method method, PostConstruct annotation) {
        if (!Modifier.isStatic(method.getModifiers())) {
            List<Key<?>> parameterKeys = Reflect.getParameterKeys(type, method);
            Object[] parameters = new Object[parameterKeys.size()];
            for (int i = 0; i < parameters.length; i++)
                parameters[i] = injector.get().getProvider(parameterKeys.get(i)).get();
            try {
                Proxy.invoker(method).invoke(instance, parameters);
            }
            catch (Exception e) {
                throw MycilaGuiceException.runtime(e);
            }
        }
    }

}
