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

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class Redis {

    private static final Logger LOGGER = Logger.getLogger(Redis.class.getName());

    private Redis() {
    }

    // per thread usage

    private static final ThreadLocal<Jedis> connection = new ThreadLocal<Jedis>();

    public static Jedis get() {
        Jedis jedis = connection.get();
        if (jedis == null) {
            throw new IllegalStateException("No Redis connection bound to local thread");
        }
        return jedis;
    }

    static boolean isEmpty() {
        return connection.get() == null;
    }

    static void set(Jedis jedis) {
        if (connection.get() != null) {
            throw new IllegalStateException("Redis connection already bound to local thread");
        }
        connection.set(jedis);
    }

    static Jedis remove() {
        Jedis jedis = get();
        connection.remove();
        return jedis;
    }

    // static usage

    private static JedisPool pool;

    public static <V, E extends Throwable> V execute(RedisCallback<V, E> cb) throws E {
        if (pool == null) {
            throw new IllegalStateException("No JedisPool injected");
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Getting Redis connection from pool...");
        }
        Jedis jedis = pool.getResource();
        try {
            return cb.execute(jedis);
        } finally {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Returning Redis connection to pool...");
            }
            pool.returnResource(jedis);
        }
    }

    @Inject
    static void setPool(JedisPool pool) {
        if (Redis.pool != null) {
            LOGGER.log(Level.WARNING, "JedisPool pool already set");
        }
        Redis.pool = pool;
    }

}
