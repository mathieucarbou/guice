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
package com.mycila.guice.ext.web;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.util.Modules;
import com.mycila.guice.ext.closeable.CloseableInjector;
import com.mycila.guice.ext.service.ServiceModule;

import jakarta.servlet.ServletContextEvent;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 * date 2013-01-30
 */
public class MycilaGuiceListener extends GuiceServletContextListener {

    private final Module[] modules;

    public MycilaGuiceListener() {
        this.modules = new Module[0];
    }

    public MycilaGuiceListener(Module... modules) {
        this.modules = modules;
    }

    @Override
    protected Injector getInjector() {
        return Guice.createInjector(Stage.PRODUCTION, Modules.override(this.modules).with(new ServiceModule(MycilaGuiceListener.class.getClassLoader())));
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        Injector injector = (Injector) servletContextEvent.getServletContext().getAttribute(Injector.class.getName());
        if (injector != null) {
            injector.getInstance(CloseableInjector.class).close();
        }
        super.contextDestroyed(servletContextEvent);
    }

}
