The following documentation is not up to date. Please help if you want to contribute!

---------------

**Table of Contents**

- [Mycila Guice Extensions](#mycila-guice-extensions)
	- [Extensions](#extensions)
		- [1. Customizes injection annotations](#1-customizes-injection-annotations)
		- [2. Closeable Injector](#2-closeable-injector)
		- [3. JSR-250](#3-jsr-250)
		- [4. Legacy and Factory Binder](#4-legacy-and-factory-binder)
		- [5. Service and Module discovery](#5-service-and-module-discovery)
		- [6. Web Extensions](#6-web-extensions)
		- [7. Groovy Extensions](#7-groovy-extensions)
		- [8. Servlet Extension](#8-servlet-extension)
	- [Get everything in one package](#get-everything-in-one-package)

# Mycila Guice Extensions #

This project contains a set of Google Guice Extensions useful in every-days development with [Google Guice](https://code.google.com/p/google-guice/).

 - __OSGi Compliant:__ <img width="100px" src="http://www.sonatype.com/system/images/W1siZiIsIjIwMTMvMDQvMTIvMTEvNDAvMzcvMTgzL05leHVzX0ZlYXR1cmVfTWF0cml4X29zZ2lfbG9nby5wbmciXV0/Nexus-Feature-Matrix-osgi-logo.png" title="OSGI Compliant"></img>
 - __Build status:__ [![Build Status](https://travis-ci.org/mycila/guice.png?branch=master)](https://travis-ci.org/mycila/guice)
 - __Issues:__ https://github.com/mycila/license-maven-plugin/issues
 - __License:__ [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
 
[![](https://badge.waffle.io/mycila/guice.svg?label=ready&title=Ready)](http://waffle.io/mycila/guice)
[![](https://badge.waffle.io/mycila/guice.svg?label=in-progress&title=In%20Progress)](http://waffle.io/mycila/guice)
[![](https://badge.waffle.io/mycila/guice.svg?label=under-review&title=Under%20Review)](http://waffle.io/mycila/guice)

__Contributors__

* [@mgoellnitz](https://github.com/mgoellnitz)
* [@keeganwitt](https://github.com/keeganwitt)

__LATEST RELEASE:__

* [3.6.ga](http://repo1.maven.org/maven2/com/mycila/guice) (2015-01-23) - see [issues and pull requests](https://github.com/mycila/guice/issues?q=milestone%3A3.6)

__Maven Repository__

 * __Releases:__ http://repo1.maven.org/maven2/com/mycila/guice/extensions/
 * __Snapshots:__ https://oss.sonatype.org/content/repositories/snapshots/com/mycila/guice/extensions/

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
        <artifactId>mycila-guice-jsr250</artifactId>
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

___Loading custom implementations from the classpath___


You can also bind an interface to one or several implementations discovered on the classpath by using the providers `SingleServiceProvider` or `MultiServiceProvider`.  

    bind(MyService.class).toProvider(new SingleServiceProvider<>(Service.class));

Or to bind all discovered implementations to an array: 

    bind(MyService[].class).toProvider(new MultiServiceProvider<>(Service.class));

Just put on your classpath the file `META-INF/services/my.package.MyService` and the list of implementations in it.

### 6. Web Extensions ###

This extension facilitate the setup of a Guice environment within a web application.

__Maven dependency__

    <dependency>
        <groupId>com.mycila.guice.extensions</groupId>
        <artifactId>mycila-guice-web</artifactId>
        <version>X.Y.ga</version>
    </dependency>

__Usage__

Just declare the `MycilaGuiceListener` as a listener in your `web.xml` file. The listener automatically creates a Guice injector by using the [Service and Module discovery extension](#5-service-and-module-discovery).

    <listener>
        <listener-class>com.mycila.guice.ext.web.MycilaGuiceListener</listener-class>
    </listener>

### 7. Groovy Extensions ###

This extension scans for classes having methods annotated by `@Expand` and add those methods to target Groovy classes.

__Maven dependency__

    <dependency>
        <groupId>com.mycila.guice.extensions</groupId>
        <artifactId>mycila-guice-groovy</artifactId>
        <version>X.Y.ga</version>
    </dependency>

__Note__

This extension is automatically discovered when using the [Service and Module discovery extension](#5-service-and-module-discovery).

__Usage__

Just add the module `ExpandModule` in your Injector. Supposing you have a repository class and a Book class:

    class Book {
        // [...]
    }
    
    class BookRepository {
    
        @Expand(Book)
        Book findById(String id) { [...] }
        
        @Expand(Book)
        Book save(Book b) { [...] }
    
    } 

Than you can now in your code execute:

    Book b = Book.findById('123')
    // [...]
    b.save()

### 8. Servlet Extension ###

This is not an extension but a repackaging

Same code as the official Google Guice Servlet Extension, but do not depend on internal Guice stuff but on external Guava dependency instead.

__Maven dependency__

    <dependency>
        <groupId>com.mycila.guice</groupId>
        <artifactId>guice-servlet</artifactId>
        <version>X.Y.ga</version>
    </dependency>

## Get everything in one package ##

If you want to get all extensions at once (but you may end up with more dependencies that you may want, so you might need to exclude some), then you can depend on:

    <dependency>
        <groupId>com.mycila.guice</groupId>
        <artifactId>mycila-guice-all</artifactId>
        <version>X.Y.ga</version>
    </dependency>

Note: the `-all` package depends on the repackaging version of Google Guice Servlet

[![githalytics.com alpha](https://cruel-carlota.pagodabox.com/cf77f56baad338e179ef83d90c026635 "githalytics.com")](http://githalytics.com/mycila/guice)
