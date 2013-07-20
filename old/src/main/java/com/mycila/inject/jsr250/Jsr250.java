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

import com.google.common.collect.Iterables;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.mycila.inject.injector.MethodHandler;
import com.mycila.inject.internal.Reflect;

import javax.annotation.PreDestroy;
import java.lang.reflect.Method;
import java.util.Arrays;

import static com.google.common.collect.Iterables.filter;
import static com.mycila.inject.internal.Reflect.annotatedBy;
import static com.mycila.inject.internal.Reflect.findMethods;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class Jsr250 {
    private Jsr250() {
    }

    public static Jsr250Injector createInjector(Module... modules) {
        return createInjector(Arrays.asList(modules));
    }

    public static Jsr250Injector createInjector(Iterable<? extends Module> modules) {
        return createInjector(Stage.DEVELOPMENT, modules);
    }

    public static Jsr250Injector createInjector(Stage stage, Module... modules) {
        return createInjector(stage, Arrays.asList(modules));
    }

    private static class DestroyModule implements Module {
        @Inject
        private Jsr250Injector injector;

        @Override
        public void configure(Binder binder) {
            binder.requestInjection(this);
        }

        void destroy() {
            if (injector != null)
                injector.destroy();
        }
    }

    public static Jsr250Injector createInjector(Stage stage, Iterable<? extends Module> modules) {
        DestroyModule destroyModule = new DestroyModule();
        modules = Iterables.concat(Arrays.asList(destroyModule), modules);
        try {
            return Guice.createInjector(
                stage,
                Iterables.concat(modules, Arrays.asList(new Jsr250Module())))
                .getInstance(Jsr250Injector.class);
        } catch (RuntimeException e) {
            destroyModule.destroy();
            throw e;
        }
    }

    /*private static boolean hasJSR250Module(Stage stage, Iterable<? extends Module> modules) {
        final Key key = Key.get(Jsr250Destroyer.class);
        for (Element element : Elements.getElements(stage, modules)) {
            Boolean res = element.acceptVisitor(new DefaultElementVisitor<Boolean>() {
                @Override
                public <T> Boolean visit(Binding<T> binding) {
                    return key.equals(binding.getKey());
                }
            });
            if (res != null && res)
                return true;
        }
        return false;
    }*/

    public static Module newJsr250Module() {
        return new Jsr250Module();
    }

    public static boolean hasJSR250Module(Injector injector) {
        return injector.getBindings().containsKey(Key.get(Jsr250Destroyer.class));
    }

    public static <T> void preDestroy(T instance) {
        TypeLiteral<T> type = (TypeLiteral<T>) TypeLiteral.get(Reflect.getTargetClass(instance));
        MethodHandler<PreDestroy> handler = new Jsr250PreDestroyHandler();
        for (Method method : filter(findMethods(type.getRawType()), annotatedBy(PreDestroy.class))) {
            handler.handle(type, instance, method, method.getAnnotation(PreDestroy.class));
        }
    }

}
