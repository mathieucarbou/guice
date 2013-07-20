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

package com.mycila.inject.scope;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.mycila.inject.annotation.ConcurrentSingleton;
import com.mycila.inject.annotation.ResetSingleton;
import com.mycila.inject.annotation.SoftSingleton;
import com.mycila.inject.annotation.WeakSingleton;

import java.util.concurrent.TimeUnit;

import static com.mycila.inject.MycilaGuice.in;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class ExtraScopeModule implements Module {
    @Override
    public void configure(Binder binder) {
        binder.bindScope(ConcurrentSingleton.class, in(binder).concurrentSingleton(20, TimeUnit.SECONDS));
        binder.bindScope(WeakSingleton.class, in(binder).weakSingleton());
        binder.bindScope(SoftSingleton.class, in(binder).softSingleton());
        binder.bindScope(ResetSingleton.class, in(binder).resetSingleton());
    }
}
