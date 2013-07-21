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
package com.mycila.guice.ext.injection;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public class MycilaGuiceException extends RuntimeException {

    public MycilaGuiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public MycilaGuiceException(Throwable cause) {
        super(cause.getMessage(), cause);
        setStackTrace(cause.getStackTrace());
    }

    public static RuntimeException toRuntime(Throwable throwable) {
        while (throwable instanceof InvocationTargetException
                || throwable instanceof ExecutionException
                || throwable instanceof MycilaGuiceException)
            throwable = throwable instanceof InvocationTargetException ?
                    ((InvocationTargetException) throwable).getTargetException() :
                    throwable.getCause();
        if (throwable instanceof Error)
            throw (Error) throwable;
        if (throwable instanceof RuntimeException)
            return (RuntimeException) throwable;
        return new MycilaGuiceException(throwable);
    }
}
