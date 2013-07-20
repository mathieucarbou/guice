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

import com.google.common.base.Supplier;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.inject.*;
import com.google.inject.spi.*;
import com.mycila.inject.annotation.Jsr250Singleton;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.util.*;

import static com.mycila.inject.MycilaGuice.in;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
final class Jsr250Module implements Module {

    private final Set<Scope> scopes = new HashSet<Scope>();
    private final Set<Class<? extends Annotation>> scopeAnnotations = new HashSet<Class<? extends Annotation>>();

    @Override
    public void configure(Binder binder) {
        binder.bind(Jsr250Injector.class).to(Jsr250InjectorImpl.class).in(Singleton.class);
        binder.bind(Jsr250KeyProvider.class).in(Singleton.class);
        binder.bind(Jsr250PostConstructHandler.class).in(Singleton.class);
        in(binder)
            .bindAnnotationInjector(Resource.class, Jsr250KeyProvider.class)
            .handleMethodAfterInjection(PostConstruct.class, Jsr250PostConstructHandler.class)
            .bind(Jsr250Destroyer.class, new Jsr250Destroyer() {
                @Inject
                Injector injector;

                @Override
                public void preDestroy() {
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
                        if (new SingletonChecker(binding).isSingleton()) {
                            close(binding, done, dependants);
                        }
                    for (Scope scope : injector.getScopeBindings().values())
                        Jsr250.preDestroy(scope);
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
                                    Jsr250.preDestroy(o);
                                    done.put(o, Void.TYPE);
                                }
                            } else if (new SingletonChecker(binding).isSingleton()) {
                                Object o = binding.getProvider().get();
                                if (!done.containsKey(o)) {
                                    Jsr250.preDestroy(o);
                                    done.put(o, Void.TYPE);
                                }
                            }
                        } catch (Exception e) {
                            // just ignore close errors
                        }
                    }
                }
            });
    }

    public Jsr250Module addCloseableScopes(Scope... scopes) {
        this.scopes.addAll(Arrays.asList(scopes));
        return this;
    }

    public Jsr250Module addCloseableScopes(Class<? extends Annotation>... scopeAnnotations) {
        this.scopeAnnotations.addAll(Arrays.asList(scopeAnnotations));
        return this;
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
            return scope.getClass().isAnnotationPresent(Jsr250Singleton.class) || scopes.contains(scope);
        }

        @Override
        public Boolean visitScopeAnnotation(Class<? extends Annotation> scopeAnnotation) {
            return scopeAnnotation.isAnnotationPresent(Jsr250Singleton.class)
                || scopeAnnotations.contains(scopeAnnotation);
        }

        @Override
        public Boolean visitNoScoping() {
            return binding instanceof ProviderInstanceBinding<?> || binding instanceof InstanceBinding<?>;
        }

    }
}
