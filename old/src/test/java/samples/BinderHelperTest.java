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

package samples;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.mycila.inject.annotation.ConcurrentSingleton;
import com.mycila.inject.annotation.ExpiringSingleton;
import com.mycila.inject.annotation.RenewableSingleton;
import com.mycila.inject.annotation.ResetSingleton;
import com.mycila.inject.annotation.SoftSingleton;
import com.mycila.inject.annotation.WeakSingleton;
import com.mycila.inject.injector.KeyProviderSkeleton;
import com.mycila.inject.jsr250.Jsr250;
import com.mycila.inject.jsr250.Jsr250Injector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import static com.mycila.inject.MycilaGuice.in;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@RunWith(JUnit4.class)
public final class BinderHelperTest {

    @Test
    public void test() throws Exception {
        Jsr250Injector jsr250Injector = Jsr250.createInjector(new MyModule(), new AbstractModule() {
            @Override
            protected void configure() {
                in(binder()).bindAnnotationInjector(Autowire.class, AutowireKeyProvider.class);
            }
        });

    }

    @Target({METHOD, CONSTRUCTOR, FIELD})
    @Retention(RUNTIME)
    static @interface Autowire {
        String value() default "";
    }

    static final class AutowireKeyProvider extends KeyProviderSkeleton<Autowire> {
        @Override
        public Key<?> getKey(TypeLiteral<?> injectedType, Field injectedMember, Autowire resourceAnnotation) {
            String name = resourceAnnotation.value();
            return name.length() == 0 ?
                    super.getKey(injectedType, injectedMember, resourceAnnotation) :
                    Key.get(injectedType.getFieldType(injectedMember), Names.named(name));
        }
    }

    static final class MyModule extends AbstractModule {
        @Override
        protected void configure() {
            bindScope(RenewableSingleton.class, in(binder()).renewableSingleton(1, TimeUnit.DAYS));
            bindScope(ExpiringSingleton.class, in(binder()).expiringSingleton(1, TimeUnit.DAYS));
            bindScope(ConcurrentSingleton.class, in(binder()).concurrentSingleton(20, TimeUnit.SECONDS));
            bindScope(ResetSingleton.class, in(binder()).resetSingleton());
            bindScope(WeakSingleton.class, in(binder()).weakSingleton());
            bindScope(SoftSingleton.class, in(binder()).softSingleton());
        }
    }

}
