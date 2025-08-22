/*
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
package com.mycila.guice.ext.injection;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.internal.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;
import static java.util.Arrays.asList;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 * date 2013-07-20
 */
public class Reflect {

    private static final Function<Signature, Method> TO_METHOD = new Function<Signature, Method>() {
        @Override
        public Method apply(Signature from) {
            return from.method;
        }
    };

    private static final List<Signature> OBJECT_METHODS = Lists.newArrayList(transform(asList(Object.class.getDeclaredMethods()), new Function<Method, Signature>() {
        @Override
        public Signature apply(Method from) {
            return new Signature(from);
        }
    }));

    private static Key<?> buildKey(TypeLiteral<?> type, Annotation[] annotations) {
        for (Annotation annotation : annotations)
            if (Annotations.isBindingAnnotation(annotation.annotationType()))
                return Key.get(type, annotation);
        return Key.get(type);
    }

    private static final LoadingCache<AnnotatedElement, Set<Class<? extends Annotation>>> ANNOT_CACHE = CacheBuilder.newBuilder()
        .weakKeys()
        .softValues()
        .build(new CacheLoader<AnnotatedElement, Set<Class<? extends Annotation>>>() {
            @Override
            public Set<Class<? extends Annotation>> load(AnnotatedElement e) throws Exception {
                Annotation[] annotations = e.getDeclaredAnnotations();
                Set<Class<? extends Annotation>> annotationTypes = new HashSet<Class<? extends Annotation>>(annotations.length);
                for (Annotation annotation : annotations) {
                    annotationTypes.add(annotation.annotationType());
                }
                return annotationTypes;
            }
        });

    private static final LoadingCache<Class<?>, List<Signature>> METHODS = CacheBuilder.newBuilder()
        .weakKeys()
        .softValues()
        .build(new CacheLoader<Class<?>, List<Signature>>() {
            @Override
            public List<Signature> load(Class<?> clazz) throws Exception {
                List<Signature> sup;
                Class<?> sc = clazz.getSuperclass();
                if (sc == null)
                    sup = Collections.emptyList();
                else if (sc == Object.class)
                    sup = OBJECT_METHODS;
                else
                    sup = METHODS.get(sc);
                Method[] methods = clazz.isInterface() ? clazz.getMethods() : clazz.getDeclaredMethods();
                final List<Signature> thisMethods = new ArrayList<Signature>(methods.length);
                for (Method method : methods) {
                    if (!(method.isSynthetic() || method.isBridge())) {
                        thisMethods.add(new Signature(method));
                    }
                }
                return Lists.newLinkedList(concat(thisMethods, Iterables.filter(sup, new Predicate<Signature>() {
                    @Override
                    public boolean apply(Signature input) {
                        int pos = thisMethods.indexOf(input);
                        if (pos == -1) return true;
                        Signature override = thisMethods.get(pos);
                        return !overrides(override.method, input.method);
                    }
                })));
            }
        });

    public static boolean isAnnotationPresent(AnnotatedElement annotatedElement, Class<? extends Annotation> annotationType) {
        try {
            return ANNOT_CACHE.get(annotatedElement).contains(annotationType);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static List<Key<?>> getParameterKeys(TypeLiteral<?> type, Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        List<TypeLiteral<?>> parameterTypes = type.getParameterTypes(method);
        List<Key<?>> keys = new ArrayList<Key<?>>(parameterTypes.size());
        for (int i = 0; i < parameterTypes.size(); i++)
            keys.add(buildKey(parameterTypes.get(i), parameterAnnotations[i]));
        return keys;
    }

    public static Iterable<MethodInvoker> findAllAnnotatedInvokables(Class<?> type, Class<? extends Annotation> annot) {
        return transform(findAllAnnotatedMethods(type, annot), new Function<Method, MethodInvoker>() {
            @Override
            public MethodInvoker apply(Method method) {
                return MethodInvoker.on(method);
            }
        });
    }

    public static Iterable<Method> findAllAnnotatedMethods(Class<?> type, Class<? extends Annotation> annot) {
        return Iterables.filter(findAllMethods(type), annotatedBy(annot));
    }

    public static Iterable<Method> findAllMethods(Class<?> type) {
        try {
            return transform(METHODS.get(type), TO_METHOD);
        } catch (ExecutionException e) {
            throw MycilaGuiceException.toRuntime(e);
        }
    }

    public static Iterable<Field> findAllAnnotatedFields(Class<?> type, Class<? extends Annotation> annot) {
        return Iterables.filter(findAllFields(type), annotatedBy(annot));
    }

    public static Iterable<Field> findAllFields(Class<?> type) {
        return type == null || type == Object.class ?
            new LinkedList<Field>() :
            concat(Lists.newArrayList(type.getDeclaredFields()), findAllFields(type.getSuperclass()));
    }

    public static Class<?> getTargetClass(Class<?> proxy) {
        if (proxy.getName().contains("$$")) {
            do {
                proxy = proxy.getSuperclass();
            } while (proxy.getName().contains("$$"));
            return proxy;
        }
        return proxy;
    }

    public static Class<?> getTargetClass(Object instance) {
        return getTargetClass(instance.getClass());
    }

    public static <T extends AnnotatedElement> Predicate<T> annotatedBy(final Class<? extends Annotation> annotationType) {
        return new Predicate<T>() {
            @Override
            public boolean apply(T element) {
                return Reflect.isAnnotationPresent(element, annotationType);
            }
        };
    }

    public static Predicate<Method> withSignature(final String methodName, final Class<?>... classes) {
        return new Predicate<Method>() {
            @Override
            public boolean apply(Method member) {
                if (!member.getName().equals(methodName)) return false;
                Class<?>[] thisParams = member.getParameterTypes();
                if (thisParams.length != classes.length)
                    return false;
                int c = 0;
                for (Class<?> thisParam : thisParams)
                    if (thisParam != classes[c++])
                        return false;
                return true;
            }
        };
    }

    /**
     * Returns true if a overrides b. Assumes signatures of a and b are the same and a's declaring
     * class is a subclass of b's declaring class.
     */
    public static boolean overrides(Method a, Method b) {
        // See JLS section 8.4.8.1
        int modifiers = b.getModifiers();
        if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
            return true;
        }
        if (Modifier.isPrivate(modifiers)) {
            return false;
        }
        // b must be package-private
        return a.getDeclaringClass().getPackage().equals(b.getDeclaringClass().getPackage());
    }

    private static final class Signature {
        public final Class[] parameterTypes;
        private final int hash;
        public final Method method;

        public Signature(Method method) {
            this.method = method;
            this.parameterTypes = method.getParameterTypes();
            int h = method.hashCode();
            h = h * 31 + parameterTypes.length;
            for (Class parameterType : parameterTypes) {
                h = h * 31 + parameterType.hashCode();
            }
            this.hash = h;
        }

        @Override
        public int hashCode() {
            return this.hash;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Signature)) {
                return false;
            }
            Signature other = (Signature) o;
            if (!method.getName().equals(other.method.getName())) {
                return false;
            }
            if (parameterTypes.length != other.parameterTypes.length) {
                return false;
            }
            for (int i = 0; i < parameterTypes.length; i++) {
                if (parameterTypes[i] != other.parameterTypes[i]) {
                    return false;
                }
            }
            return true;
        }
    }

}
