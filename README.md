Dropwizard/Spring/SpringMVC(optional)
===================================

Welcome to the Dropwizard/Spring project.


Introduction
------------

[Dropwizard](http://dropwizard.codahale.com) is a Java framework for developing ops-friendly, high-performance, RESTful web services.

[Spring](http://www.springsource.org/spring-framework) is the most popular application development framework for enterprise Java™.

This project provide a simple method for integrating Spring with Dropwizard.

It is also possible to run the dropwizard application next to a Spring MVC eg. as under the /api context.

Versions
------------

The current version of the project is **0.2**.

| dropwizard-spring-mvc   | Dropwizard   | Spring        |
|:-----------------------:|:------------:|:-------------:|
| master(0.4.0-SNAPSHOT)  | 0.6.2        | 3.2.4.RELEASE |
| dropwizard-spring(0.3.1)| 0.6.2        | 3.1.4.RELEASE |
| dropwizard-spring 0.2   | 0.6.0        | 3.1.3.RELEASE |
| dropwizard-spring 0.1   | 0.5.1        | 3.1.1.RELEASE |


Installation
------------


To install Dropwizard/Spring you just have to add this Maven dependency in your project :

```xml
<dependency>
     <groupId>com.github.exclam</groupId>
     <artifactId>dropwizard-spring-mvc</artifactId>
     <version>0.4.0-SNAPSHOT</version>
</dependency>
```

Usage
------------

The Dropwizard/Spring integration allow to automatically initialize Dropwizard environment through a Spring application applicationContext including health checks, resources, providers, tasks and managed.

To use Dropwizard/Spring you just have to add a ```SpringBundle``` and create your Spring application applicationContext.

For example :

```java

    public class HelloApp extends Service<HelloAppConfiguration> {

        private static final String CONFIGURATION_FILE = "src/test/resources/hello/hello.yml";

        public static void main(String[] args) throws Exception {
          new HelloApp().run(new String[]{"server", CONFIGURATION_FILE});
        }

        @Override
        public void initialize(Bootstrap<HelloAppConfiguration> bootstrap) {
          // register configuration, environment and placeholder
          bootstrap.addBundle(new SpringBundle(applicationContext(), true, true, true));
        }

        @Override
        public void run(HelloAppConfiguration configuration, Environment environment) throws Exception {
          // doing nothing
        }


        private ConfigurableApplicationContext applicationContext() throws BeansException {
          AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
          applicationContext.scan("hello");
          return applicationContext;
        }
    }

```

In this example we create a Spring application applicationContext based on annotation to resolve Spring beans.

The ```SpringBundle``` class use the application applicationContext to initialize Dropwizard environment including health checks, resources, providers, tasks and managed.

Moreover the ```SpringBundle``` class register :

 - a ```ConfigurationPlaceholderConfigurer``` to resolve Dropwizard configuration as [Spring placeholders](http://static.springsource.org/spring/docs/3.1.x/spring-framework-reference/html/beans.html#beans-factory-placeholderconfigurer) (For example : ```${http.port}```).

 - the Dropwizard configuration with the name ```dw``` to retrieve complex configuration with [Spring Expression Language](http://static.springsource.org/spring/docs/3.1.x/spring-framework-reference/html/expressions.html) (For example : ```#{dw.httpConfiguration}```).

 - the Dropwizard environment with the name ```dwEnv``` to retrieve complex configuration with [Spring Expression Language](http://static.springsource.org/spring/docs/3.1.x/spring-framework-reference/html/expressions.html) (For example : ```#{dwEnv.validator}```).

Please take a look at the hello application located in ```src/test/java/hello```.

Spring MVC
------------

In order to run the application besides a SpringMVC application you have to modify the sourcecode above by

 - calling the "webapp" command.
 - enabling the webApplication, with putting the dropwizard jersey servlet under a specific context path
 - the AppConfiguration.class is the main app context configuration. Here the security should be defined.

```java

    ...
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Parameter expected: [/path/to/configuration.yaml]");
            return;
        }
        new HelloApp().run(new String[]{"webapp", args[0]});
    }

    @Override
    public void initialize(Bootstrap<FrontConfiguration> bootstrap) {
        // register configuration, environment and placeholder
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(AppConfiguration.class);
        bootstrap.addBundle(new SpringBundle<FrontConfiguration>(applicationContext)
                .enableWebApplication(this, instantiateWebAppInitializers(), "/api"));
    }

    private List<WebApplicationInitializer> instantiateWebAppInitializers() {
        List<WebApplicationInitializer> webApplicationInitializers = new ArrayList<>();
        webApplicationInitializers.add(new SecurityWebApplicationInitializer());
        webApplicationInitializers.add(new WebAppInitializer());
        return webApplicationInitializers;
    }
    ...

```

 - A root application context should not be set. It is done in our service.
 - The WebConfiguration.class is your Configuration class for the WebApp, which is annotated with @EnableWebMvc
    - in this configuration the Controller should be defined. (eg. by component scan)

WebAppInitializer:
```java

    public class WebAppInitializer extends
            AbstractAnnotationConfigDispatcherServletInitializer {

        @Override
        protected Class<?>[] getRootConfigClasses() {
            return null;
        }

        @Override
        protected Class<?>[] getServletConfigClasses() {
            return new Class<?>[]{WebConfiguration.class};
        }

        @Override
        protected String[] getServletMappings() {
            return new String[]{"/"};
        }
    }

```

 - The security web app initializer is a spring standard for registering the springSecurityFilterChain.

SecurityWebApplicationInitializer:
```java

   public class SecurityWebApplicationInitializer
           extends AbstractSecurityWebApplicationInitializer {
   }

```


## JSP Servlet security tags
 Due to the fact, that it is not possible to get a list of beans of a generic type from the parent of an application context
 by calling the getBeansOfType(Type) method, we added a fix in case the JSP security tags library is included into the project.
 This fix injects the SecurityExpressionHandlers of the main application into the web-application-Context.
 This is done completely transient, so you just can use the tags as usual.

## jetty-springmvc-jsp-template
 Some code is taken initially from the following project, but there were major changes necessary in using the ideas in dropwizard.

 [jasonish on github](https://github.com/jasonish/jetty-springmvc-jsp-template)


License
------------

    The Apache Software License, Version 2.0
    http://www.apache.org/licenses/LICENSE-2.0.txt
