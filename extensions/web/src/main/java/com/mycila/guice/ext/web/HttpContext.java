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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Deque;
import java.util.LinkedList;
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
            return new LinkedList<>();
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

    public static HttpContext get() {
        try {
            return CTX.get().getFirst();
        } catch (NoSuchElementException e) {
            throw new IllegalStateException("No HttpContext bound to local thread ! Surround your code with HttpContext.enter() or HttpContextFilter !");
        }
    }

}
