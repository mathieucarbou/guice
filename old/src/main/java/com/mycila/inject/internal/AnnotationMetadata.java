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
package com.mycila.inject.internal;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
class AnnotationMetadata<T extends Annotation> {

    private static final boolean hasAnnotationType;
    private static final Map<Class<?>, Object> DEFAULTS = new HashMap<Class<?>, Object>(16) {
        {
            put(boolean.class, false);
            put(char.class, '\0');
            put(byte.class, (byte) 0);
            put(short.class, (short) 0);
            put(int.class, 0);
            put(long.class, 0L);
            put(float.class, 0f);
            put(double.class, 0d);
        }
    };

    static {
        hasAnnotationType = hasAnnotationType(AnnotationMetadata.class.getClassLoader());
    }

    private static boolean hasAnnotationType(ClassLoader classLoader) {
        try {
            classLoader.loadClass("sun.reflect.annotation.AnnotationType");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private final Class<T> type;
    private final Map<String, Object> properties;
    private transient volatile Method[] memberMethods;

    AnnotationMetadata(Class<T> type, Map<String, Object> properties) {
        this.type = type;
        this.properties = properties;
    }

    /* public */

    public Class<T> getType() {
        return type;
    }

    public Object get(String property) {
        Object result = properties.get(property);
        if (result == null)
            throw new IncompleteAnnotationException(type, property);
        return result.getClass().isArray() && Array.getLength(result) != 0 ?
                cloneArray(result) :
                result;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(128);
        result.append('@');
        result.append(type.getName());
        result.append('(');
        boolean firstMember = true;
        for (Map.Entry<String, Object> e : properties.entrySet()) {
            if (firstMember)
                firstMember = false;
            else
                result.append(", ");
            result.append(e.getKey());
            result.append('=');
            result.append(memberValueToString(e.getValue()));
        }
        result.append(')');
        return result.toString();
    }

    @Override
    public int hashCode() {
        int result = 0;
        for (Map.Entry<String, Object> e : properties.entrySet())
            result += (127 * e.getKey().hashCode()) ^ memberValueHashCode(e.getValue());
        return result;
    }

    /* private */

    private boolean isEqual(Object o) {
        if (!type.isInstance(o))
            return false;
        if (AnnotationHandler.class.isInstance(o) && ((AnnotationHandler) o).metadata == this)
            return true;
        for (Method memberMethod : getMemberMethods()) {
            String member = memberMethod.getName();
            Object ourValue = get(member);
            Object hisValue;
            AnnotationHandler hisHandler = asOneOfUs(o);
            if (hisHandler != null) {
                hisValue = hisHandler.metadata.get(member);
            } else {
                try {
                    hisValue = memberMethod.invoke(o);
                } catch (InvocationTargetException e) {
                    return false;
                } catch (IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            }
            if (!memberValueEquals(ourValue, hisValue))
                return false;
        }
        return true;
    }

    private AnnotationHandler asOneOfUs(Object o) {
        if (Proxy.isProxyClass(o.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(o);
            if (handler instanceof AnnotationHandler)
                return (AnnotationHandler) handler;
        }
        return null;
    }

    private Method[] getMemberMethods() {
        if (memberMethods == null) {
            Method[] mm = type.getDeclaredMethods();
            AccessibleObject.setAccessible(mm, true);
            memberMethods = mm;
        }
        return memberMethods;
    }

    /* constructors */

    @SuppressWarnings({"unchecked"})
    public static <T extends Annotation> T buildRandomAnnotation(Class<T> annotationClass) {
        return (T) Proxy.newProxyInstance(
                annotationClass.getClassLoader(),
                new Class<?>[]{annotationClass},
                new AnnotationHandler(AnnotationMetadata.randomAnnotation(annotationClass)));
    }

    @SuppressWarnings({"unchecked"})
    public static <T extends Annotation> T buildAnnotation(Class<T> annotationClass, Map<String, Object> properties) {
        return (T) Proxy.newProxyInstance(
                annotationClass.getClassLoader(),
                new Class<?>[]{annotationClass},
                new AnnotationHandler(AnnotationMetadata.randomAnnotation(annotationClass, properties)));
    }

    /* utilities - copied from Sun internal class */

    @SuppressWarnings({"ConstantConditions"})
    private static boolean memberValueEquals(Object v1, Object v2) {
        Class type = v1.getClass();
        // Check for primitive, string, class, enum const, annotation,
        // or ExceptionProxy
        if (!type.isArray())
            return v1.equals(v2);
        // Check for array of string, class, enum const, annotation,
        // or ExceptionProxy
        if (v1 instanceof Object[] && v2 instanceof Object[])
            return Arrays.equals((Object[]) v1, (Object[]) v2);
        // Check for ill formed annotation(s)
        if (v2.getClass() != type)
            return false;
        // Deal with array of primitives
        if (type == byte[].class)
            return Arrays.equals((byte[]) v1, (byte[]) v2);
        if (type == char[].class)
            return Arrays.equals((char[]) v1, (char[]) v2);
        if (type == double[].class)
            return Arrays.equals((double[]) v1, (double[]) v2);
        if (type == float[].class)
            return Arrays.equals((float[]) v1, (float[]) v2);
        if (type == int[].class)
            return Arrays.equals((int[]) v1, (int[]) v2);
        if (type == long[].class)
            return Arrays.equals((long[]) v1, (long[]) v2);
        if (type == short[].class)
            return Arrays.equals((short[]) v1, (short[]) v2);
        assert type == boolean[].class;
        return Arrays.equals((boolean[]) v1, (boolean[]) v2);
    }

    private static Object cloneArray(Object array) {
        Class type = array.getClass();
        if (type == byte[].class) {
            byte[] byteArray = (byte[]) array;
            return byteArray.clone();
        }
        if (type == char[].class) {
            char[] charArray = (char[]) array;
            return charArray.clone();
        }
        if (type == double[].class) {
            double[] doubleArray = (double[]) array;
            return doubleArray.clone();
        }
        if (type == float[].class) {
            float[] floatArray = (float[]) array;
            return floatArray.clone();
        }
        if (type == int[].class) {
            int[] intArray = (int[]) array;
            return intArray.clone();
        }
        if (type == long[].class) {
            long[] longArray = (long[]) array;
            return longArray.clone();
        }
        if (type == short[].class) {
            short[] shortArray = (short[]) array;
            return shortArray.clone();
        }
        if (type == boolean[].class) {
            boolean[] booleanArray = (boolean[]) array;
            return booleanArray.clone();
        }
        Object[] objectArray = (Object[]) array;
        return objectArray.clone();
    }

    private static int memberValueHashCode(Object value) {
        Class type = value.getClass();
        if (!type.isArray())    // primitive, string, class, enum const, or annotation
            return value.hashCode();
        if (type == byte[].class)
            return Arrays.hashCode((byte[]) value);
        if (type == char[].class)
            return Arrays.hashCode((char[]) value);
        if (type == double[].class)
            return Arrays.hashCode((double[]) value);
        if (type == float[].class)
            return Arrays.hashCode((float[]) value);
        if (type == int[].class)
            return Arrays.hashCode((int[]) value);
        if (type == long[].class)
            return Arrays.hashCode((long[]) value);
        if (type == short[].class)
            return Arrays.hashCode((short[]) value);
        if (type == boolean[].class)
            return Arrays.hashCode((boolean[]) value);
        return Arrays.hashCode((Object[]) value);
    }

    private static String memberValueToString(Object value) {
        Class type = value.getClass();
        if (!type.isArray())    // primitive, string, class, enum const, or annotation
            return value.toString();
        if (type == byte[].class)
            return Arrays.toString((byte[]) value);
        if (type == char[].class)
            return Arrays.toString((char[]) value);
        if (type == double[].class)
            return Arrays.toString((double[]) value);
        if (type == float[].class)
            return Arrays.toString((float[]) value);
        if (type == int[].class)
            return Arrays.toString((int[]) value);
        if (type == long[].class)
            return Arrays.toString((long[]) value);
        if (type == short[].class)
            return Arrays.toString((short[]) value);
        if (type == boolean[].class)
            return Arrays.toString((boolean[]) value);
        return Arrays.toString((Object[]) value);
    }

    private static <T extends Annotation> AnnotationMetadata<T> randomAnnotation(Class<T> annotationClass) {
        return randomAnnotation(annotationClass, new LinkedHashMap<String, Object>());
    }

    private static <T extends Annotation> AnnotationMetadata<T> randomAnnotation(Class<T> annotationClass, Map<String, Object> properties) {
        Map<String, Object> allProps = new LinkedHashMap<String, Object>();
        Map<String, Object> defaults = hasAnnotationType ? getAnnotationDefaults(annotationClass) : new HashMap<String, Object>();
        Method[] mm = annotationClass.getDeclaredMethods();
        AccessibleObject.setAccessible(mm, true);
        for (Method method : mm) {
            String name = method.getName();
            if (properties.containsKey(name))
                allProps.put(name, properties.get(name));
            else {
                Object o = defaults.get(name);
                if (o == null) o = getTypeDefaults(method.getReturnType());
                allProps.put(name, o);
            }
        }
        return new AnnotationMetadata<T>(annotationClass, allProps);
    }

    private static Object getTypeDefaults(Class<?> type) {
        Object o = DEFAULTS.get(type);
        if (o != null) return o;
        // this is a class, enum, String or array
        if (type.isArray()) return Array.newInstance(type.getComponentType(), 0);
        if (type == String.class) return "";
        if (type == Class.class) return Void.class;
        try {
            if (type.isEnum()) return Enum.valueOf((Class<? extends Enum>) type, type.getDeclaredFields()[0].getName());
        } catch (Exception e) {
            throw new UnsupportedOperationException("Unable to randomize annotation: cannot get first enum of " + type);
        }
        throw new UnsupportedOperationException("Type: " + type.getName());
    }

    private static Map<String, Object> getAnnotationDefaults(Class<? extends Annotation> annotationClass) {
        Map<String, Object> defaults = new LinkedHashMap<String, Object>();
        try {
            Field field = Class.class.getDeclaredField("annotationType");
            field.setAccessible(true);
            sun.reflect.annotation.AnnotationType type = (sun.reflect.annotation.AnnotationType) field.get(annotationClass);
            defaults.putAll(type.memberDefaults());
        } catch (Exception ignored) {
        }
        return defaults;
    }

    /* Inner class, JDK proxy interceptor */

    private static class AnnotationHandler implements InvocationHandler, Serializable {
        final AnnotationMetadata metadata;

        public AnnotationHandler(AnnotationMetadata metadata) {
            this.metadata = metadata;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String member = method.getName();
            Class[] paramTypes = method.getParameterTypes();
            // Handle Object and Annotation methods
            if (member.equals("equals") && paramTypes.length == 1 && paramTypes[0] == Object.class)
                return metadata.isEqual(args[0]);
            if (member.equals("toString"))
                return metadata.toString();
            if (member.equals("hashCode"))
                return metadata.hashCode();
            if (member.equals("annotationType"))
                return metadata.getType();
            // Handle annotation member accessors
            return metadata.get(member);
        }
    }

}
