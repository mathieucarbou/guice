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
package com.mycila.guice.ext.jsr250;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.mycila.guice.ext.injection.KeyProviderSkeleton;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.lang.reflect.Field;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
class Jsr250KeyProvider extends KeyProviderSkeleton<Resource> {

    @Inject Injector injector;

    @Override
    public Key<?> getKey(TypeLiteral<?> injectedType, Field injectedMember, Resource resourceAnnotation) {
        String name = resourceAnnotation.name();

        if (name.length() != 0) {
            // explicit key

            // if a name is provided, it acts as a Named binding and this means we ask for a precise key
            return Key.get(injectedType.getFieldType(injectedMember), Names.named(name));

        } else {
            // implicit key

            // if no name given, try a combination with the field name
            Key<?> implicitKey = Key.get(injectedType.getFieldType(injectedMember), Names.named(name));

            if (injector.getExistingBinding(implicitKey) != null) {
                return implicitKey;

            } else {
                // else create the find based on the field type (default behavior) - with optional existing binding annotations
                return super.getKey(injectedType, injectedMember, resourceAnnotation);
            }

        }
    }
}
