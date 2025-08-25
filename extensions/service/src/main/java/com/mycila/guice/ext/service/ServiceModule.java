/*
 * Copyright (C) 2010-2025 Mycila (mathieu.carbou@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mycila.guice.ext.service;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public class ServiceModule implements Module {

    private final ClassLoader classLoader;

    public ServiceModule() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public ServiceModule(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void configure(Binder binder) {
        List<Module> runtime = new LinkedList<Module>();
        List<Module> overrides = new LinkedList<Module>();
        for (Module module : ServiceLoader.load(Module.class, classLoader)) {
            if (module.getClass().isAnnotationPresent(OverrideModule.class))
                overrides.add(module);
            else
                runtime.add(module);
        }
        if (overrides.isEmpty()) {
            for (Module module : runtime) {
                binder.install(module);
            }
        } else {
            binder.install(Modules.override(runtime).with(overrides));
        }
    }

}
