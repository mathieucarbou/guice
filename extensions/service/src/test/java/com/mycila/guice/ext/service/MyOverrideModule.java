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
package com.mycila.guice.ext.service;

import com.google.inject.AbstractModule;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 * date 2013-07-21
 */
@OverrideModule
public final class MyOverrideModule extends AbstractModule {
    static int hit;

    @Override
    protected void configure() {
        hit++;
        bind(Serv.class).to(ServB.class);
    }
}
