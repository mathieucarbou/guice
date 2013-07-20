package com.mycila.guice.ext.injection;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.google.common.collect.Iterables.concat;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 * @date 2013-07-20
 */
public final class TypeInfo<T> {

    private final TypeLiteral<T> type;

    private TypeInfo(TypeLiteral<T> type) {
        this.type = type;
    }

    public TypeLiteral<T> getType() {
        return type;
    }

    public Class<? super T> getRawType() {
        return type.getRawType();
    }

    public List<Key<?>> getParameterKeys(TypeLiteral<?> type, Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        List<TypeLiteral<?>> parameterTypes = type.getParameterTypes(method);
        List<Key<?>> keys = new ArrayList<>(parameterTypes.size());
        for (int i = 0; i < parameterTypes.size(); i++)
            keys.add(buildKey(parameterTypes.get(i), parameterAnnotations[i]));
        return keys;
    }

    public Iterable<Method> findAllAnnotatedMethods(Class<? extends Annotation> annot) {
        return Iterables.filter(findAllMethods(), annotatedBy(annot));
    }

    public List<Method> findAllMethods() {
        ClassInfo.TYPES.get(getRawType()).findAllMethods();

        if (methods == null) {
            final List<Method> thisMethods = new ArrayList<>();
            for (Method method : (getRawType().isInterface() ? getRawType().getMethods() : getRawType().getDeclaredMethods())) {
                if (!(method.isSynthetic() || method.isBridge())) {
                    thisMethods.add(method);
                }
            }
            methods = Lists.newLinkedList(getRawType().getSuperclass() == null ? thisMethods : concat(thisMethods, Iterables.filter(TypeInfo.of(type.getSupertype(getRawType().getSuperclass())).findAllMethods(), new Predicate<Method>() {
                @Override
                public boolean apply(Method m) {
                    int pos = thisMethods.indexOf(m);
                    if (pos == -1) return true;
                    Method override = thisMethods.get(pos);
                    return !overrides(override, m);
                }
            })));
        }
        return methods;
    }

    public Iterable<Field> findAllAnnotatedFields(Class<? extends Annotation> annot) {
        return Iterables.filter(findAllFields(), annotatedBy(annot));
    }

    public List<Field> findAllFields() {
        if (fields == null) {
            List<Field> fields = new LinkedList<>();
            Class<?> type = this.getRawType();
            while (type != null && type != Object.class) {
                Collections.addAll(fields, type.getDeclaredFields());
                type = type.getSuperclass();
            }
            this.fields = fields;
        }
        return fields;
    }

    public static <T> TypeInfo<T> of(TypeLiteral<T> type) {
        return of(type.getRawType());
    }

    public static <T> TypeInfo<T> of(T instance) {
        return of(instance.getClass());
    }

    public static <T> TypeInfo<T> of(Class<?> type) {
        if (type.getName().contains("$$")) {
            do {
                type = type.getSuperclass();
            } while (type.getName().contains("$$"));
        }
        try {
            return (TypeInfo<T>) TYPES.get(type);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static <T extends AnnotatedElement> Predicate<T> annotatedBy(final Class<? extends Annotation> annotationType) {
        return new Predicate<T>() {
            @Override
            public boolean apply(T element) {
                return element.isAnnotationPresent(annotationType);
            }
        };
    }

    /**
     * Returns true if a overrides b. Assumes signatures of a and b are the same and a's declaring
     * class is a subclass of b's declaring class.
     */
    private static boolean overrides(Method a, Method b) {
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

    private static Key<?> buildKey(TypeLiteral<?> type, Annotation[] annotations) {
        for (Annotation annotation : annotations)
            if (Annotations.isBindingAnnotation(annotation.annotationType()))
                return Key.get(type, annotation);
        return Key.get(type);
    }

    private static final class ClassInfo {

        private final Class<?> type;
        private List<Field> fields;
        private List<Method> methods;

        public ClassInfo(Class<?> type) {
            this.type = type;
        }

        public List<Method> findAllMethods() {
            if (methods == null) {
                final List<Method> thisMethods = new ArrayList<>();
                for (Method method : (type.isInterface() ? type.getMethods() : type.getDeclaredMethods())) {
                    if (!(method.isSynthetic() || method.isBridge())) {
                        thisMethods.add(method);
                    }
                }
                methods = Lists.newLinkedList(type.getSuperclass() == null ? thisMethods : concat(thisMethods, Iterables.filter(TypeInfo.of(type.getSupertype(getRawType().getSuperclass())).findAllMethods(), new Predicate<Method>() {
                    @Override
                    public boolean apply(Method m) {
                        int pos = thisMethods.indexOf(m);
                        if (pos == -1) return true;
                        Method override = thisMethods.get(pos);
                        return !overrides(override, m);
                    }
                })));
            }
            return methods;
        }

        private static final LoadingCache<Class<?>, ClassInfo> TYPES = CacheBuilder.newBuilder()
            .weakKeys()
            .softValues()
            .build(new CacheLoader<Class<?>, ClassInfo>() {
                @Override
                public ClassInfo load(Class<?> key) throws Exception {
                    return new ClassInfo(key);
                }
            });

    }
}
