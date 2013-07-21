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

package com.mycila.guice.ext.jsr250;

import com.google.common.base.Supplier;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.inject.Binder;
import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Scope;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.BindingScopingVisitor;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.HasDependencies;
import com.google.inject.spi.InstanceBinding;
import com.google.inject.spi.ProviderInstanceBinding;
import com.mycila.guice.ext.injection.MBinder;
import com.mycila.guice.ext.injection.MethodHandler;
import com.mycila.guice.ext.injection.Reflect;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class Jsr250Module implements Module {

    @Override
    public void configure(Binder binder) {
        binder.bind(Jsr250Injector.class).to(Jsr250InjectorImpl.class).in(Singleton.class);
        binder.bind(Jsr250KeyProvider.class).in(Singleton.class);
        binder.bind(Jsr250PostConstructHandler.class).in(Singleton.class);
        binder.bind(new TypeLiteral<MethodHandler<PreDestroy>>() {
        }).to(Jsr250PreDestroyHandler.class).in(Singleton.class);
        MBinder.wrap(binder)
            .bindAnnotationInjector(Resource.class, Jsr250KeyProvider.class)
            .handleMethodAfterInjection(PostConstruct.class, Jsr250PostConstructHandler.class)
            .bind(Jsr250Destroyer.class, new Jsr250Destroyer() {
                @Inject
                Injector injector;

                @Inject
                MethodHandler<PreDestroy> destroyer;

                @Override
                public void destroy() {
                    Map<Key<?>, Binding<?>> bindings = injector.getAllBindings();
                    Multimap<Binding<?>, Binding<?>> dependants = Multimaps.newSetMultimap(new IdentityHashMap<Binding<?>, Collection<Binding<?>>>(), new Supplier<Set<Binding<?>>>() {
                        @Override
                        public Set<Binding<?>> get() {
                            return new HashSet<>();
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
                    Map<Object, Object> done = new IdentityHashMap<>(bindings.size());
                    for (final Binding<?> binding : bindings.values())
                        if (new SingletonChecker(binding).isSingleton()) {
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
                            } else if (new SingletonChecker(binding).isSingleton()) {
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

            });
    }

    private class SingletonChecker implements BindingScopingVisitor<Boolean> {

        private final Binding<?> binding;

        private SingletonChecker(Binding<?> binding) {
            this.binding = binding;
        }

        boolean isSingleton() {
            return Scopes.isSingleton(binding) || binding.acceptScopingVisitor(this);
        }

        @Override
        public Boolean visitEagerSingleton() {
            return true;
        }

        @Override
        public Boolean visitScope(Scope scope) {
            return false;
        }

        @Override
        public Boolean visitScopeAnnotation(Class<? extends Annotation> scopeAnnotation) {
            return false;
        }

        @Override
        public Boolean visitNoScoping() {
            return binding instanceof ProviderInstanceBinding<?> || binding instanceof InstanceBinding<?>;
        }

    }

}
