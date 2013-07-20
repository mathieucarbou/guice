package com.mycila.guice.ext.injection;

import com.google.inject.Binder;
import com.google.inject.matcher.Matchers;

import java.lang.annotation.Annotation;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 * @date 2013-07-20
 */
public final class Injection {

    private final Binder binder;

    private Injection(Binder binder) {
        this.binder = binder;
    }

    public <A extends Annotation> Injection bindAnnotationInjector(Class<A> annotationType, Class<? extends KeyProvider<A>> providerClass) {
        binder.bindListener(Matchers.any(), requestInjection(new MemberInjectorTypeListener<A>(annotationType, providerClass)));
        return this;
    }

    public <A extends Annotation> Injection handleMethodAfterInjection(Class<A> annotationType, Class<? extends MethodHandler<A>> providerClass) {
        binder.bindListener(Matchers.any(), requestInjection(new MethodHandlerTypeListener<A>(annotationType, providerClass)));
        return this;
    }

    public <A extends Annotation> Injection handleFieldAfterInjection(Class<A> annotationType, Class<? extends FieldHandler<A>> providerClass) {
        binder.bindListener(Matchers.any(), requestInjection(new FieldHandlerTypeListener<A>(annotationType, providerClass)));
        return this;
    }

    public <A extends Annotation> Injection handleAfterInjection(Class<A> annotationType, Class<? extends AnnotatedMemberHandler<A>> providerClass) {
        binder.bindListener(Matchers.any(), requestInjection(new AnnotatedMemberHandlerTypeListener<A>(annotationType, providerClass)));
        return this;
    }

    public <T> T requestInjection(T object) {
        binder.requestInjection(object);
        return object;
    }

    public <T> Injection bind(Class<T> type, T instance) {
        binder.bind(type).toInstance(requestInjection(instance));
        return this;
    }

    public static Injection on(Binder binder) {
        return new Injection(binder);
    }
}
