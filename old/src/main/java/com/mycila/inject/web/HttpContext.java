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

package com.mycila.inject.web;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.servlet.RequestParameters;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class HttpContext {

    private static final Logger LOGGER = Logger.getLogger(HttpContext.class.getName());

    private static final ThreadLocal<Deque<HttpContext>> CTX = new ThreadLocal<Deque<HttpContext>>() {
        @Override
        protected Deque<HttpContext> initialValue() {
            return new LinkedList<HttpContext>();
        }
    };

    public static final Module MODULE = new Module() {
        @Override
        public void configure(Binder binder) {
        }

        @Provides
        HttpServletRequest provideHttpServletRequest() {
            return getContext().request;
        }

        @Provides
        HttpServletResponse provideHttpServletResponse() {
            return getContext().response;
        }

        @Provides
        HttpSession provideHttpSession() {
            return getContext().request.getSession();
        }

        @Provides
        @RequestParameters
        Map<String, String[]> provideRequestParameters() {
            return getContext().request.getParameterMap();
        }

    };

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    private HttpContext(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    public HttpServletRequest request() {
        return request;
    }

    public HttpServletResponse response() {
        return response;
    }

    public static void change(HttpServletRequest request, HttpServletResponse response) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("[" + request.getRequestURI() + "] Changing HttpContext to request " + request.getClass().getName() + " and response " + response.getClass().getName());
        }
        HttpContext context = new HttpContext(request, response);
        CTX.get().offerFirst(context);
    }


    public static void end() {
        CTX.get().poll();
    }

    public static HttpContext getContext() {
        try {
            return CTX.get().getFirst();
        } catch (NoSuchElementException e) {
            throw new IllegalStateException("No HttpContext bound to local thread ! Surround your code with HttpContext.enter() or HttpContextFilter !");
        }
    }

}
