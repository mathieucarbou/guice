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

package com.mycila.inject.util;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public class DefaultLoader implements Loader {

    private final ClassLoader classLoader;

    public DefaultLoader() {
        this(getDefaultClassLoader());
    }

    public DefaultLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Class<?> loadClass(String className) {
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to load class " + className, e);
        }
    }

    @Override
    public URL getResource(String path) {
        return classLoader.getResource(path);
    }

    @Override
    public List<URL> getResources(String path) {
        List<URL> urls = new LinkedList<URL>();
        try {
            Enumeration<URL> e = classLoader.getResources(path);
            while (e.hasMoreElements())
                urls.add(e.nextElement());
        } catch (IOException ignored) {
        }
        return urls;
    }

    private static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        }
        catch (Throwable ignored) {
        }
        if (cl == null)
            cl = DefaultLoader.class.getClassLoader();
        return cl;
    }

}