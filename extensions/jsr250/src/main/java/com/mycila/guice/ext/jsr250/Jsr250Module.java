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

import com.google.common.base.Supplier;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.inject.*;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.HasDependencies;
import com.google.inject.spi.ProviderInstanceBinding;
import com.mycila.guice.ext.closeable.CloseableInjector;
import com.mycila.guice.ext.closeable.InjectorCloseListener;
import com.mycila.guice.ext.injection.MBinder;
import com.mycila.guice.ext.injection.MethodHandler;
import com.mycila.guice.ext.injection.Reflect;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public class Jsr250Module extends AbstractModule {

    @Override
    public void configure() {
        requireBinding(CloseableInjector.class);
        MyJsr250Destroyer destroyer = new MyJsr250Destroyer();
        requestInjection(destroyer);
        bind(MyJsr250Destroyer.class).toInstance(destroyer);
        bind(Jsr250KeyProvider.class).in(Singleton.class);
        bind(Jsr250PostConstructHandler.class).in(Singleton.class);
        bind(new TypeLiteral<MethodHandler<PreDestroy>>() {
        }).to(Jsr250PreDestroyHandler.class).in(Singleton.class);
        MBinder.wrap(binder())
            .bindAnnotationInjector(Resource.class, Jsr250KeyProvider.class)
            .handleMethodAfterInjection(PostConstruct.class, Jsr250PostConstructHandler.class);
    }

    static class MyJsr250Destroyer implements InjectorCloseListener {
        @Inject
        Injector injector;

        @Inject
        MethodHandler<PreDestroy> destroyer;

        @Override
        public void onInjectorClosing() {
            Map<Key<?>, Binding<?>> bindings = injector.getAllBindings();
            Multimap<Binding<?>, Binding<?>> dependants = Multimaps.newSetMultimap(new IdentityHashMap<Binding<?>, Collection<Binding<?>>>(), new Supplier<Set<Binding<?>>>() {
                @Override
                public Set<Binding<?>> get() {
                    return new HashSet<Binding<?>>();
                }
            });
            for (Binding<?> binding : bindings.values()) {
                if (binding instanceof HasDependencies) {
                    for (Dependency<?> dependency : ((HasDependencies) binding).getDependencies()) {
                        if (bindings.containsKey(dependency.getKey())) {
                            dependants.put(injector.getBinding(dependency.getKey()), binding);
                        }
                    }
                }
            }
            Map<Object, Object> done = new IdentityHashMap<Object, Object>(bindings.size());
            for (final Binding<?> binding : bindings.values())
                if (Scopes.isSingleton(binding)) {
                    close(binding, done, dependants);
                }
            for (Scope scope : injector.getScopeBindings().values())
                preDestroy(scope);
        }

        private void close(Binding<?> binding, Map<Object, Object> done, Multimap<Binding<?>, Binding<?>> dependants) {
            if (!done.containsKey(binding)) {
                done.put(binding, Void.TYPE);
                for (Binding<?> dependant : dependants.get(binding)) {
                    close(dependant, done, dependants);
                }
                try {
                    if (binding instanceof ProviderInstanceBinding<?>) {
                        Object o = ((ProviderInstanceBinding) binding).getProviderInstance();
                        if (!done.containsKey(o)) {
                            preDestroy(o);
                            done.put(o, Void.TYPE);
                        }
                    } else if (Scopes.isSingleton(binding)) {
                        Object o = binding.getProvider().get();
                        if (!done.containsKey(o)) {
                            preDestroy(o);
                            done.put(o, Void.TYPE);
                        }
                    }
                } catch (Exception e) {
                    // just ignore close errors
                }
            }
        }

        private void preDestroy(Object instance) {
            TypeLiteral<?> type = TypeLiteral.get(Reflect.getTargetClass(instance));
            for (Method method : Reflect.findAllAnnotatedMethods(type.getRawType(), PreDestroy.class)) {
                destroyer.handle(type, instance, method, method.getAnnotation(PreDestroy.class));
            }
        }
    }

}
