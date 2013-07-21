**Table of Contents**  *generated with [DocToc](http://doctoc.herokuapp.com/)*

- [Mycila Guice Extensions](#mycila-guice-extensions)
	- [Maven Repository](#maven-repository)
	- [Build Status](#build-status)
	- [Extensions](#extensions)
		- [1. Customizes injection annotations](#1-customizes-injection-annotations)
		- [2. Closeable Injector](#2-closeable-injector)
		- [3. JSR-250](#3-jsr-250)
		- [4. Legacy and Factory Binder](#4-legacy-and-factory-binder)
		- [5. Service and Module discovery](#5-service-and-module-discovery)
		- [6. Web Extensions](#6-web-extensions)
- [](#)

# Mycila Guice Extensions #

This project contains a set of Google Guice Extensions useful in every-days development with [Google Guice](https://code.google.com/p/google-guice/).

## Maven Repository ##

 - __Releases__ 

Available in Maven Central Repository: http://repo1.maven.org/maven2/com/mycila/guice/extensions/

 - __Snapshots__
 
Available in OSS Repository:  https://oss.sonatype.org/content/repositories/snapshots/com/mycila/guice/extensions/

## Build Status ##

[![Build Status](https://travis-ci.org/mycila/guice.png?branch=master)](https://travis-ci.org/mycila/guice)

## Extensions ##

### 1. Customizes injection annotations ###

This extensions enables you to define custom injection annotations and use them. This extensions is used by the [JSR-250 extension](#3-jsr-250).  

__Maven dependency__

    <dependency>
        <groupId>com.mycila.guice.extensions</groupId>
        <artifactId>mycila-guice-injection</artifactId>
        <version>X.Y.ga</version>
    </dependency>

__Usage__

In example, suppose you have your own annotation called `@Autowire` to inject dependencies. You could automatically support `@Resource`, `@Inject` and `@Autowire` at the same time. Supposing you'd like to use this annotation to inject your dependencies:

    @Target({METHOD, CONSTRUCTOR, FIELD})
    @Retention(RUNTIME)
    public @interface Autowire {
        String value() default "";
    }

You have to define a `KeyProvider` which help creating the Guice key used to recover a dependency from the annotation information plus the injected member.

    public class AutowireKeyProvider extends KeyProviderSkeleton<Autowire> {
        @Override
        public Key<?> getKey(TypeLiteral<?> injectedType, Field injectedMember, Autowire resourceAnnotation) {
            String name = resourceAnnotation.value();
            return name.length() == 0 ?
                    super.getKey(injectedType, injectedMember, resourceAnnotation) :
                    Key.get(injectedType.getFieldType(injectedMember), Names.named(name));
        }
    }

Once the key provider is defined, just add this code in your Guice module:

    MBinder.wrap(binder()).bindAnnotationInjector(Autowire.class, AutowireKeyProvider.class);



### 2. Closeable Injector ###

This extension allows your classes to listen when an Injector is closed, to be able to clean some resources for example.

__Maven dependency__

    <dependency>
        <groupId>com.mycila.guice.extensions</groupId>
        <artifactId>mycila-guice-closeable</artifactId>
        <version>X.Y.ga</version>
    </dependency>

__Note__

This extension is automatically loaded if you are using the [Service and Module discovery extension](#5-service-and-module-discovery).

__Usage__

Bind in your module the classes you want to be aware of Injector closing. Those classes must implement the `InjectorCloseListener` interface. 

    public interface InjectorCloseListener {
        void onInjectorClosing();
    }

Create your Injector has usual and add the ` CloseableModule`.

    Injector injector = Guice.createInjector(Stage.PRODUCTION, new CloseableModule(), new MyModule());

Or like this:

    CloseableInjector injector = Guice.createInjector(Stage.PRODUCTION, new CloseableModule(), new MyModule()).getInstance(CloseableInjector.class);

The `CloseableInjector` is juste the plain standard Injector enhanced with a `close()` method. You can use it instead of the default `Injector`.

When your application ends, just close the Injector like this if you are using the `CloseableInjector`: 

    injector.close()

Or if you are using Guice's `Injector` class:

    injector.getInstance(CloseableInjector.class).close();

### 3. JSR-250 ###

This extension adds JSR-250 (object life-cycle) support to Guice.

__Maven dependency__

    <dependency>
        <groupId>com.mycila.guice.extensions</groupId>
        <artifactId>mycila-guice-closeable</artifactId>
        <version>X.Y.ga</version>
    </dependency>

__Notes__

This extension depends on the [Closeable Injector extension](#2-closeable-injector) and both can be automatically automatically loaded if you are using the [Service and Module discovery extension](#5-service-and-module-discovery).

`@PreDestroy` only works for singletons.

__Usage__

Create your `Injector` with those two modules also: `Jsr250Module` and `CloseableModule`.

    Injector injector = Guice.createInjector(Stage.PRODUCTION, new CloseableModule(), new Jsr250Module(), new MyModule());

if you are using the [Service and Module discovery extension](#5-service-and-module-discovery), you just need to create the Injector like this as usual.

    Injector injector = Guice.createInjector(Stage.PRODUCTION, new MyModule());

And that's all you need to have you `@PostConstruct`, `@PreDestroy` and `@Resource` annotations working!

Do not forget when you have finished working with the `Injector` to close it so that `@PreDestroy` methods get called. 

    injector.getInstance(CloseableInjector.class).close();

__Example of JSR-250 class__

    @Singleton
    public class Bank {
    
        List<Account> accounts = new ArrayList<Account>();
    
        @Resource
        Provider<Account> provider;
    
        @PostConstruct
        void openBank() {
            // create two accounts initially
            accounts.add(provider.get());
            accounts.add(provider.get());
        }
    
        @PreDestroy
        void close() {
            accounts.clear();
        }
    }

### 4. Legacy and Factory Binder ###

This extension allows the binding easily of legacy code or objects build through a factory method. 

__Maven dependency__

    <dependency>
        <groupId>com.mycila.guice.extensions</groupId>
        <artifactId>mycila-guice-legacy</artifactId>
        <version>X.Y.ga</version>
    </dependency>

__Usage__

Suppose that you have the following classes having an old-way designed with factory classes:

    public interface Repository {
        // [...]
    }

And its factory:

    public class ServiceFactory {
        
        public void setOption(String option) {
            // [...]
        }

        public Repository newRepository(Connection con) { 
            // [...] (code using option to return a Repository) 
        }

    }

By using the `LegacyProvider` of this extension you can bind the `Repository` like this in your Guice module:

    bind(Repository.class).toProvider(LegacyProvider.of(Repository.class)
        .withFactory(ServiceFactory.class, "create", Connection.class)
        .inject("setOption", String.class)
    );

This enables Guice to load and inject the `ServiceFactory` and get all the parameters also from the Guice bindings.

### 5. Service and Module discovery ###

This extension allows the discovery of Guice module automatically in the classpath by using the JDK Service Loader feature. You can also bind custom interfaces and automatically discover and inject into the implementations defined on the classpath.

Since automatic discovery does not allow you to control bindings, this extension comes with an `@OverrideModule` annotation to be able to flag modules which overrides existing bindings.

__Maven dependency__

    <dependency>
        <groupId>com.mycila.guice.extensions</groupId>
        <artifactId>mycila-guice-service</artifactId>
        <version>X.Y.ga</version>
    </dependency>

__Usage__

___Loading Guice module from the classpath___

Put a file in your classpath called `com.google.inject.Module` in the `META-INF/services` folder containing the complete class names of your modules. In example:

    # In `META-INF/services/com.google.inject.Module`
    com.mycila.guice.ext.service.MyModule
    com.mycila.guice.ext.service.MyOverrideModule

Then load your `Injector` with the `ServiceModule`:

    Injector injector = Guice.createInjector(new ServiceModule());

This will also add the two other module in your `Injector`.

___Loading custom implementation from the classpath___


### 6. Web Extensions ###




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

