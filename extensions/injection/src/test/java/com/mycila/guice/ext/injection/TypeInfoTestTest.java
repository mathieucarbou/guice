package com.mycila.guice.ext.injection;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 * @date 2013-07-21
 */
@RunWith(JUnit4.class)
public class TypeInfoTestTest {

    @Test
    public void test() throws Exception {
        assertEquals(15, describe(Reflect.findAllMethods(getClass())).size());
        assertEquals(0, describe(Reflect.findAllFields(getClass())).size());
        assertEquals(1, describe(Reflect.findAllAnnotatedMethods(getClass(), Test.class)).size());
        assertEquals(1, describe(Reflect.findAllAnnotatedInvokables(getClass(), Test.class)).size());
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