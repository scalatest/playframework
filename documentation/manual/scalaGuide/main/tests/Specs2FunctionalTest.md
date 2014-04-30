# Writing functional tests

Play provides a number of classes and convenience methods that assist with functional testing.  Most of these can be found either in the [`play.api.test`](api/scala/index.html#play.api.test.package) package or in the [`Helpers`](api/scala/index.html#play.api.test.Helpers$) object.

You can add these methods and classes by importing the following:

```scala
import play.api.test._
import play.api.test.Helpers._
```

## FakeApplication

Play frequently requires a running [`Application`](api/scala/index.html#play.api.Application) as context: it is usually provided from [`play.api.Play.current`](api/scala/index.html#play.api.Play$).

To provide an environment for tests, Play provides a [`FakeApplication`](api/scala/index.html#play.api.test.FakeApplication) class which can be configured with a different Global object, additional configuration, or even additional plugins.

@[scalatest-fakeApplication](code/ScalaFunctionalTestSpec.scala)

## WithApplication

To pass in an application to an example, use [`WithApplication`](api/scala/index.html#play.api.test.WithApplication).  An explicit [`FakeApplication`](api/scala/index.html#play.api.test.FakeApplication) can be passed in, but a default [`FakeApplication`](api/scala/index.html#play.api.test.FakeApplication) is provided for convenience.

Because [`WithApplication`](api/scala/index.html#play.api.test.WithApplication) is a built in [`Around`](http://etorreborre.github.io/specs2/guide/org.specs2.guide.Structure.html#Around) block, you can override it to provide your own data population:

@[scalafunctionaltest-withdbdata](code/WithDbDataSpec.scala)

## WithServer

Sometimes you want to test the real HTTP stack from with your test, in which case you can start a test server using [`WithServer`](api/scala/index.html#play.api.test.WithServer):

@[scalafunctionaltest-testpaymentgateway](code/ScalaFunctionalTestSpec.scala)

The `port` value contains the port number the server is running on.  By default this is 19001, however you can change this either by passing the port into the with [`WithServer`](api/scala/index.html#play.api.test.WithServer) constructor, or by setting the system property `testserver.port`.  This can be useful for integrating with continuous integration servers, so that ports can be dynamically reserved for each build.

A [`FakeApplication`](api/scala/index.html#play.api.test.FakeApplication) can also be passed to the test server, which is useful for setting up custom routes and testing WS calls:

@[scalafunctionaltest-testws](code/ScalaFunctionalTestSpec.scala)

## WithBrowser

If you want to test your application using a browser, you can use [Selenium WebDriver](http://code.google.com/p/selenium/?redir=1). Play will start the WebDriver for your, and wrap it in the convenient API provided by [FluentLenium](https://github.com/FluentLenium/FluentLenium).

```scala
"run in a browser" in new WithBrowser {
  browser.goTo("/")
  browser.$("#title").getTexts().get(0) must equalTo("Hello Guest")

  browser.$("a").click()

  browser.url must equalTo("/")
  browser.$("#title").getTexts().get(0) must equalTo("Hello Coco")
}
```

Like [`WithServer`](api/scala/index.html#play.api.test.WithServer), you can change the port, [`FakeApplication`](api/scala/index.html#play.api.test.FakeApplication), and you can also select the web browser to use:

```scala
"run in a browser" in new WithBrowser(webDriver = FIREFOX) {
  ...
}
```

## PlaySpecification

[`PlaySpecification`](api/scala/index.html#play.api.test.PlaySpecification) excludes some of the mixins provided in the default specs2 specification that clash with Play helpers methods.  It also mixes in the Play test helpers and types for convenience.

@[scalatest-playspecification](code/ExamplePlaySpecificationSpec.scala)

## Testing a template

Since a template is a standard Scala function, you can execute it from your test, and check the result:

@[scalatest-functionaltemplatespec](code/FunctionalTemplateSpec.scala)

## Testing a controller

You can call any `Action` code by providing a [`FakeRequest`](api/scala/index.html#play.api.test.FakeRequest):

@[scalatest-functionalexamplecontrollerspec](code/FunctionalExampleControllerSpec.scala)

## Testing the router

Instead of calling the `Action` yourself, you can let the `Router` do it:

@[scalafunctionaltest-respondtoroute](code/ScalaFunctionalTestSpec.scala)

## Testing a model

If you are using an SQL database, you can replace the database connection with an in-memory instance of an H2 database using `inMemoryDatabase`.

@[scalafunctionaltest-testmodel](code/ScalaFunctionalTestSpec.scala)

> **Next:** [[Advanced topics|Iteratees]]
