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
package com.google.inject.servlet;

import com.google.inject.Singleton;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;

/**
 * This default pipeline simply dispatches to web.xml's servlet pipeline.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 * @see ManagedFilterPipeline See Also ManagedFilterPipeline.
 */
@Singleton
class DefaultFilterPipeline implements FilterPipeline {
    public void initPipeline(ServletContext context) {
    }

    public void destroyPipeline() {
    }

    public void dispatch(ServletRequest request, ServletResponse response,
                         FilterChain proceedingFilterChain) throws IOException, ServletException {

        proceedingFilterChain.doFilter(request, response);
    }
}
