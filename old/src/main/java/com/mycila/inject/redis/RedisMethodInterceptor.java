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
package com.mycila.inject.redis;

import com.google.inject.Binder;
import com.google.inject.matcher.Matchers;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import redis.clients.jedis.JedisPool;

import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class RedisMethodInterceptor implements MethodInterceptor {

    private static final Logger LOGGER = Logger.getLogger(RedisMethodInterceptor.class.getName());

    @Inject
    JedisPool pool;

    private RedisMethodInterceptor() {
    }

    public static void bind(Binder binder) {
        binder.requestStaticInjection(Redis.class);
        RedisMethodInterceptor interceptor = new RedisMethodInterceptor();
        binder.requestInjection(interceptor);
        binder.bindInterceptor(Matchers.any(), Matchers.annotatedWith(RedisCall.class), interceptor);
        binder.bindInterceptor(Matchers.annotatedWith(RedisCall.class), Matchers.any(), interceptor);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        boolean empty = Redis.isEmpty();
        if (empty) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Getting Redis connection from pool...");
            }
            Redis.set(pool.getResource());
        }
        try {
            return invocation.proceed();
        } finally {
            if (empty) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Returning Redis connection to pool...");
                }
                pool.returnResource(Redis.remove());
            }
        }
    }
}
