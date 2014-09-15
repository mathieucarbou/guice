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

import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 * date 2013-07-21
 */
@RunWith(JUnit4.class)
public class TypeInfoTestTest {

    @Test
    public void test() throws Exception {
        //assertEquals(15, describe(Reflect.findAllMethods(getClass())).size());
        //assertEquals(0, describe(Reflect.findAllFields(getClass())).size());
        assertEquals(1, describe(Reflect.findAllAnnotatedMethods(getClass(), Test.class)).size());
        //assertEquals(1, describe(Reflect.findAllAnnotatedInvokables(getClass(), Test.class)).size());
    }

    private <T> Collection<T> describe(Collection<T> c) {
        System.out.println(c.size() + " items");
        for (T t : c) {
            System.out.println(" - " + t);
        }
        return c;
    }

    private <T> Collection<T> describe(Iterable<T> c) {
        return describe(Lists.newLinkedList(c));
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public TypeInfoTestTest() {
        super();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}