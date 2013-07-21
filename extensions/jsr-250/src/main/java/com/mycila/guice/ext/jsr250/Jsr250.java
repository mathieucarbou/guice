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

import com.google.common.collect.Iterables;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Stage;

import java.util.Arrays;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class Jsr250 {

    private Jsr250() {
    }

    /**
     * Creates an injector for the given set of modules.
     *
     * @throws com.google.inject.CreationException
     *          if one or more errors occur during injector
     *          construction
     */
    public static Jsr250Injector createInjector(Module... modules) {
        return createInjector(Arrays.asList(modules));
    }

    /**
     * Creates an injector for the given set of modules.
     *
     * @throws com.google.inject.CreationException
     *          if one or more errors occur during injector
     *          creation
     */
    public static Jsr250Injector createInjector(Iterable<? extends Module> modules) {
        return createInjector(Stage.DEVELOPMENT, modules);
    }

    /**
     * Creates an injector for the given set of modules, in a given development
     * stage.
     *
     * @throws com.google.inject.CreationException
     *          if one or more errors occur during injector
     *          creation.
     */
    public static Jsr250Injector createInjector(Stage stage, Module... modules) {
        return createInjector(stage, Arrays.asList(modules));
    }

    /**
     * Creates an injector for the given set of modules, in a given development
     * stage.
     *
     * @throws com.google.inject.CreationException
     *          if one or more errors occur during injector
     *          construction
     */
    public static Jsr250Injector createInjector(Stage stage, Iterable<? extends Module> modules) {
        return Guice.createInjector(
            stage,
            Iterables.concat(modules, Arrays.asList(new Jsr250Module())))
            .getInstance(Jsr250Injector.class);
    }

}
