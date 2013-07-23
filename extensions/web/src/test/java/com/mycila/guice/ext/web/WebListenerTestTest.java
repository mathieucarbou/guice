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

import com.ovea.tajin.server.Container;
import com.ovea.tajin.server.ContainerConfiguration;
import com.ovea.tajin.server.Server;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 * @date 2013-07-22
 */
@RunWith(JUnit4.class)
public final class WebListenerTestTest {

    @Test
    public void test() throws Exception {
        Container c = ContainerConfiguration.create()
            .port(9876)
            .webappRoot("src/test/webapp")
            .buildContainer(Server.JETTY9);
        c.start();
        c.stop();
    }

}