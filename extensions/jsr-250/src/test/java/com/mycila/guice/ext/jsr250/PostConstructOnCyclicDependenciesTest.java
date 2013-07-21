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

package com.mycila.guice.ext.jsr250;

import com.google.inject.AbstractModule;
import com.google.inject.ProvisionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PostConstructOnCyclicDependenciesTest {
    Jsr250Injector inj;

    @Before
    public void setUp() throws Exception {
        inj = Jsr250.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(A.class).to(AImpl.class);
                bind(B.class).to(BImpl.class);
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        Jsr250Injector dying = inj;
        inj = null;
        dying.close();
    }

    @Test
    public void testPostConstructOnCyclicDependency() {
        A a = null;
        try {
            a = inj.getInstance(A.class);
        } catch (ProvisionException ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        assertNotNull(a);
        assertTrue(a.hasBeenCalled());
    }
}
