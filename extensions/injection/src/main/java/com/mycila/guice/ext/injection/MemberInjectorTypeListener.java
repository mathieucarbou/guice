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

package com.mycila.guice.ext.injection;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.MembersInjector;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.mycila.inject.internal.Proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class MemberInjectorTypeListener<A extends Annotation> implements TypeListener {

    private final Class<A> annotationType;
    private final Class<? extends KeyProvider<A>> providerClass;

    public MemberInjectorTypeListener(Class<A> annotationType, Class<? extends KeyProvider<A>> providerClass) {
        this.annotationType = annotationType;
        this.providerClass = providerClass;
    }

    @Override
    public <I> void hear(final TypeLiteral<I> injectableType, TypeEncounter<I> encounter) {
        final Provider<? extends KeyProvider<A>> provider = encounter.getProvider(providerClass);
        final Provider<Injector> injectorProvider = encounter.getProvider(Injector.class);
        final List<Field> fields = newLinkedList(TypeInfo.of(injectableType).findAllAnnotatedFields(annotationType));
        final List<Method> methods = newLinkedList(TypeInfo.of(injectableType).findAllAnnotatedMethods(annotationType));
        if (!fields.isEmpty() || !methods.isEmpty()) {
            encounter.register(new MembersInjector<I>() {
                @Override
                public void injectMembers(I injectee) {
                    KeyProvider<A> keyProvider = provider.get();
                    // inject fields
                    for (Field field : fields) {
                        Object value = injectorProvider.get().getProvider(keyProvider.getKey(injectableType, field, field.getAnnotation(annotationType))).get();
                        if (!field.isAccessible())
                            field.setAccessible(true);
                        try {
                            field.set(injectee, value);
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException("Failed to inject field " + field + ". Reason: " + e.getMessage(), e);
                        }
                    }
                    // inject methods
                    for (Method method : methods) {
                        List<Key<?>> parameterKeys = keyProvider.getParameterKeys(injectableType, method, method.getAnnotation(annotationType));
                        Object[] parameters = new Object[parameterKeys.size()];
                        for (int i = 0; i < parameters.length; i++)
                            parameters[i] = injectorProvider.get().getProvider(parameterKeys.get(i)).get();
                        try {
                            Proxy.invoker(method).invoke(injectee, parameters);
                        } catch (Exception e) {
                            throw runtime(e);
                        }
                    }
                }
            });
        }
    }
}
