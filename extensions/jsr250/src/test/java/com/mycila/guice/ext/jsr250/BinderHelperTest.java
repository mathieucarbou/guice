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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.mycila.guice.ext.closeable.CloseableInjector;
import com.mycila.guice.ext.closeable.CloseableModule;
import com.mycila.guice.ext.injection.KeyProviderSkeleton;
import com.mycila.guice.ext.injection.MBinder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@RunWith(JUnit4.class)
public class BinderHelperTest {

    @Autowire
    CloseableInjector jsr250Injector;

    @Test
    public void test() throws Exception {
        assertNull(jsr250Injector);
         Guice.createInjector(new Jsr250Module(), new CloseableModule(), new AbstractModule() {
             @Override
             protected void configure() {
                 MBinder.wrap(binder()).bindAnnotationInjector(Autowire.class, AutowireKeyProvider.class);
                 requestInjection(BinderHelperTest.this);
             }
         });
        assertNotNull(jsr250Injector);
    }

    @Target({METHOD, CONSTRUCTOR, FIELD})
    @Retention(RUNTIME)
    static @interface Autowire {
        String value() default "";
    }

    static class AutowireKeyProvider extends KeyProviderSkeleton<Autowire> {
        @Override
        public Key<?> getKey(TypeLiteral<?> injectedType, Field injectedMember, Autowire resourceAnnotation) {
            String name = resourceAnnotation.value();
            return name.length() == 0 ?
                super.getKey(injectedType, injectedMember, resourceAnnotation) :
                Key.get(injectedType.getFieldType(injectedMember), Names.named(name));
        }
    }

}
