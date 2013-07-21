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
package com.mycila.inject.schedule;

import com.google.inject.AbstractModule;
import com.google.inject.Stage;
import com.mycila.guice.ext.jsr250.Jsr250;
import com.mycila.guice.ext.jsr250.Jsr250Injector;
import org.junit.Test;

import javax.inject.Singleton;

public class ScheduleTest {

    private static int count;

    @Test
    public void test() throws Exception {
        Jsr250Injector injector = Jsr250.createInjector(Stage.PRODUCTION, new SchedulingModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(MyJob.class).in(Singleton.class);
            }
        });
        Thread.sleep(4000);
        injector.destroy();
    }

    @Cron("*/1 * * * * ? *")
    public static final class MyJob implements Runnable {

        @Override
        public void run() {
            count++;
            System.out.println("run: " + count);
            System.out.println("waiting 500ms...");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("interrupted !");
            }
            if (count > 2) {
                throw new RuntimeException("hello world");
            }
        }
    }
}
