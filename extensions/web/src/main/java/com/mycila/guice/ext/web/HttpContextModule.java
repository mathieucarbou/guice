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
package com.mycila.guice.ext.web;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.servlet.RequestParameters;
import com.mycila.guice.ext.service.OverrideModule;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 * @date 2013-07-21
 */
@OverrideModule
public class HttpContextModule extends AbstractModule {
    @Override
    public void configure() {
    }

    @Provides
    public HttpServletRequest provideHttpServletRequest() {
        return HttpContext.get().request();
    }

    @Provides
    public HttpServletResponse provideHttpServletResponse() {
        return HttpContext.get().response();
    }

    @Provides
    public HttpSession provideHttpSession() {
        return HttpContext.get().request().getSession();
    }

    @Provides
    @RequestParameters
    public Map<String, String[]> provideRequestParameters() {
        //noinspection unchecked
        return HttpContext.get().request().getParameterMap();
    }
}
