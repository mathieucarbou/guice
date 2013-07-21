#summary Mycila Guice

<wiki:toc max_depth="5" />

= Introduction =

MycilaGuice is an extension library to [http://code.google.com/p/google-guice/ Google Guice 3] which adds many features such as:

 contributions:
 * JSR250 (`@Resource`, `@PreDestroy`, `@PostConstruct`)
 * Additional scopes
 * Testing integration
 * Service loader facilities
 * Binding helpers
 * Support of legacy classes (classes you cannot modify to add @Inject)

The project can be used with Guice 2 or 3 and supports JSR330 (javax.inject)

= Download =

Mycila Guice is deployed in maven 2 Central Repository:

http://repo2.maven.org/maven2/com/mycila/mycila-guice/

{{{
<dependency>
    <groupId>com.mycila</groupId>
    <artifactId>mycila-guice</artifactId>
    <version>X.Y</version>
</dependency>
}}}

 * [http://code.google.com/p/mycila/source/browse/#svn/mycila-guice/trunk Browse source code]
 * [http://mycila.googlecode.com/svn/mycila-guice/trunk/ Checkout URL]

Snapshots and releases are also availables at
 * https://mc-repo.googlecode.com/svn/maven2/snapshots/com/mycila/mycila-guice/
 * https://mc-repo.googlecode.com/svn/maven2/releases/com/mycila/mycila-guice/

= Mycila Guice Features =

== JSR250 ==

=== Creating your injector ===

Using the plain old Guice way, by using the `Jsr250Module`.

{{{
Injector injector = Guice.createInjector(Stage.PRODUCTION, new MyModule(), new Jsr250Module());
}}}

Or by using the `Jsr250` helper class, which will return you a `Jsr250Injector`, which is a subclass of Guice's Injector.

{{{
Jsr250Injector jsr250Injector = Jsr250.createInjector(Stage.PRODUCTION, new MyModule());
}}}

We hardly recommend to use the later form because you can manage the lifecycle of the JSR250 injector and objects more easily with the destroy() method:

{{{
public interface Jsr250Injector extends Injector {
    void destroy();
}
}}}

=== Injecting using `@Resource` ===

You can inject fields and methods as usual, and also benefits of Guice's binding annotations and providers.

Fields are always injected before methods.

{{{
public class Account {

    @Resource
    Bank bank;

    String number;

    @Resource
    void init(Client client, @Named("RNG") Provider<Id> rng) {
        number = bank.id() + "" + client.id() + "" + rng.get().id();
    }

    public String getNumber() {
        return number;
    }
}
}}}

=== Controlling lifecycle with `@PostConstruct` and `@PreDestroy` ===

You can annotate no argument methods with these annotation.
 * Methods annotated by `@PostConstruct` will be executed after injection is completed.
 * Methods annotated by `@PreDestroy` will be executed when closing the injector

{{{
@Singleton
public class Bank {

    List<Account> accounts = new ArrayList<Account>();

    @Resource
    Provider<Account> provider;

    @PostConstruct
    void openBank() {
        // create two account initially
        accounts.add(provider.get());
        accounts.add(provider.get());
    }

    @PreDestroy
    void closeBank() {
        accounts.clear();
    }

    int id() { return 2; }
    List<Account> accounts() { return accounts; }
}
}}}

Post construction methods are handled automatically. But `@PreDestroy` methods can only be called on singleton instances or scoped instances, when closing the `Jsr250Injector`.

If you don't use the `Jsr250Injector`, you need to retreive the `Jsr250Destroyer` to destroy singletons before closing the application.

Here is the two ways. When using the `Jsr250Injector`:

{{{
jsr250Injector.destroy();
}}}

Or when using Guice's default injector:

{{{
injector.getInstance(Jsr250Destroyer.class).preDestroy();
}}}

== Binding helpers ==

=== Overview ===

`BinderHelper` is a class which help you deal more easily with Mycila Guice features. To use is, you can do a static import of this class:

{{{
import static com.mycila.inject.BinderHelper.in;
}}}

Then in your module you can use:

{{{
// Bind an instance to a class and also request injection on that instance
in(binder()).bind(MyClass, myInstance);

// Bind several interceptors at once, and also request injection on them
in(binder()).bindInterceptor(classMatcher, methodMatcher, interceptor1, interceptor2)

// Add support for an external annotation to be used for post injection handlers
in(binder()).bindAfterInjection(PostConstruct.class, Jsr250MethodHandler.class);

// Add support for an external annotation to be used to inject members besides the usual @Inject
in(binder()).bindAnnotationInjector(Resource.class, ResourceKeyProvider.class)

// Manually bind some additional scopes
bindScope(RenewableSingleton.class, in(binder()).renewableSingleton(1, TimeUnit.DAYS));
bindScope(ExpiringSingleton.class, in(binder()).expiringSingleton(1, TimeUnit.DAYS));
bindScope(ConcurrentSingleton.class, in(binder()).concurrentSingleton(20, TimeUnit.SECONDS));
bindScope(ResetSingleton.class, in(binder()).resetSingleton());
bindScope(WeakSingleton.class, in(binder()).weakSingleton());
bindScope(SoftSingleton.class, in(binder()).softSingleton());
}}}

Most of the calls are chainable (fluent api). In example:

{{{
in(binder)
    .bindAnnotationInjector(Resource.class, Jsr250KeyProvider.class)
    .bindAfterInjection(PostConstruct.class, Jsr250PostConstructHandler.class)
    .bind(Jsr250Destroyer.class, new Jsr250Destroyer());
}}}

=== Customizing injection annotation ===

In example, suppose you have your own annotation called `@Autowire` to inject dependencies. You could automatically support `@Resource`, `@Inject` and `@Autowire` at the same time. Supposing you'd like to use this annotation:

{{{
@Target({METHOD, CONSTRUCTOR, FIELD})
@Retention(RUNTIME)
public @interface Autowire {
    String value() default "";
}
}}}

You have to define a `KeyProvider` which help creating the Guice key used to recover a dependency from the annotation information plus the injected member.

{{{
static final class AutowireKeyProvider extends KeyProviderSkeleton<Autowire> {
    @Override
    public Key<?> getKey(TypeLiteral<?> injectedType, Field injectedMember, Autowire resourceAnnotation) {
        String name = resourceAnnotation.value();
        return name.length() == 0 ?
                super.getKey(injectedType, injectedMember, resourceAnnotation) :
                Key.get(injectedType.getFieldType(injectedMember), Names.named(name));
    }
}
}}}

{{{
Jsr250Injector jsr250Injector = Jsr250.createInjector(new MyModule(), new AbstractModule() {
    @Override
    protected void configure() {
        in(binder()).bindAnnotationInjector(Autowire.class, AutowireKeyProvider.class);
    }
});
}}}

== Additional scopes ==

You can install additional scopes in one way by running one of these two lines:

{{{
Jsr250Injector jsr250Injector = Jsr250.createInjector(new ExtraScopeModule(), new MyModule());

Injector injector = Guice.createInjector(new ExtraScopeModule(), new MyModule());
}}}

This module installs these additional scopes:
 * `@ConcurrentSingleton` (with a thread timeout of 20 seconds)
 * `@WeakSingleton`
 * `@SoftSingleton`
 * `@ResetSingleton`

Two other scopes are available to be bound manually:
 * `@ExpiringSingleton`
 * `@RenewableSingleton`

== Legacy support ==

== Service Loader ==

{{{
binder.bind(AgentPlugin[].class)
    .toProvider(ServiceLoaderProvider.of(AgentPlugin.class).withClassLoader(ClassLoader.class))
    .in(Cached20Seconds.class);
}}}

This binding creates in a cached scope for 20 seconds a list of AgentPlugin instances on the classpath, loaded by the JDK ServiceLoader (META-INF/services/...). When loaded, each service will be injected with its dependencies.

*or*

{{{
install(ServiceLoaderModule.withClassLoader(ClassLoader.class).of(AgentPlugin.class));
}}}

In this case. the module creates a binding of key `Set<AgentPlugin>` containing all loaded and injected instances from the META-INF/services definitions.

`withClassLoader` is optional and takes as parameter the KEY of the binding where to get the classloader. In my case, i have a binding of type ClassLoader which points to the ClassLaoder instance i want to use to discover the services.

== Testing integration ==


[![Build Status](https://travis-ci.org/mycila/guice.png?branch=master)](https://travis-ci.org/mycila/guice)
