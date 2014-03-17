<!--- Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com> -->
# Testing your application with ScalaTest

Writing tests for your application can be an involved process. Play provides an integration library, [ScalaTest + Play](http://scalatest.org/plus/play), as well as helpers and application stubs to make testing your application as easy as possible.

## Overview

The location for tests is in the "test" folder.  <!-- There are two sample test files created in the test folder which can be used as templates. -->

You can run tests from the Play console.

* To run all tests, run `test`.
* To run only one test class, run `test-only` followed by the name of the class i.e. `test-only my.namespace.MySpec`.
* To run only the tests that have failed, run `test-quick`.
* To run tests continually, run a command with a tilde in front, i.e. `~test-quick`.
* To access test helpers such as `FakeApplication` in console, run `test:console`.

Testing in Play is based on SBT, and a full description is available in the [testing SBT](http://www.scala-sbt.org/0.13.0/docs/Detailed-Topics/Testing) chapter.

## Using ScalaTest + Play

In [ScalaTest + Play](http://scalatest.org/plus/play), you define test classes by extending the PlaySpec trait. Here's an example:

@[scalatest-stackspec](code/scalatest/StackSpec.scala)

You can, of course, also [create your own base classes](http://scalatest.org/user_guide/defining_base_classes) instead of using `PlaySpec`.

You can run your tests with Play itself, or in IntelliJ IDEA (using the [Scala plugin](http://blog.jetbrains.com/scala/)) or in Eclipse (using the [Scala IDE](http://scala-ide.org/) and the [ScalaTest Eclipse plugin](http://scalatest.org/user_guide/using_scalatest_with_eclipse)).  Please see the [[IDE page|IDE]] for more details.

### Matchers

`PlaySpec` mixes in ScalaTest's [`MustMatchers`](http://doc.scalatest.org/2.1.0/index.html#org.scalatest.MustMatchers), so you can write assertions using ScalaTest's matchers DSL:

```scala
"Hello world" must endWith ("world")
```

For more information, see the documentation for [`MustMatchers`](http://doc.scalatest.org/2.1.0/index.html#org.scalatest.MustMatchers).

### Mockito

You can use mocks to isolate unit tests against external dependencies.  For example, if your class depends on an external `DataService` class, you can feed appropriate data to your class without instantiating a `DataService` object.

ScalaTest provides integration with [Mockito](https://code.google.com/p/mockito/) via its [`MockitoSugar`](http://doc.scalatest.org/2.1.0/index.html#org.scalatest.mock.MockitoSugar) trait.

To use Mockito, mix `MockitoSugar` into your test class:

```scala
class ExampleSpec extends PlaySpec with MockitoSugar // ...
```

and then add the [library dependency](http://mvnrepository.com/artifact/org.mockito/mockito-core) to the build.

Using Mockito, you can mock out references to classes like so:

@[scalaws-mockitosugar](code/scalatest/ExampleMockitoSpec.scala)

Mocking is especially useful for testing the public methods of classes.  Mocking objects and private methods is possible, but considerably harder.

## Unit Testing Models

Play does not require models to use a particular database data access layer.  However, if the application uses Anorm or Slick, then frequently the Model will have a reference to database access internally.

```scala
import anorm._
import anorm.SqlParser._

case class User(id: String, name: String, email: String) {
   def roles = DB.withConnection { implicit connection =>
      ...
    }
}
```

For unit testing, this approach can make mocking out the `roles` method tricky.

A common approach is to keep the models isolated from the database and as much logic as possible, and abstract database access behind a repository layer.

@[scalatest-models](code/models/User.scala)

@[scalatest-repository](code/services/UserRepository.scala)

```scala
class AnormUserRepository extends UserRepository {
  import anorm._
  import anorm.SqlParser._

  def roles(user:User) : Set[Role] = {
    ...
  }
}
```

and then access them through services:

@[scalatest-userservice](code/services/UserService.scala)

In this way, the `isAdmin` method can be tested by mocking out the `UserRepository` reference and passing it into the service:

@[scalatest-userservicespec](code/scalatest/UserServiceSpec.scala)

## Unit Testing Controllers

Controllers are defined as objects in Play, and so can be trickier to unit test.  In Play this can be alleviated by [[dependency injection|ScalaDependencyInjection]] using [`getControllerInstance`](api/scala/index.html#play.api.GlobalSettings@getControllerInstance).  Another way to finesse unit testing with a controller is to use a trait with an [explicitly typed self reference](http://www.naildrivin5.com/scalatour/wiki_pages/ExplcitlyTypedSelfReferences) to the controller:

@[scalatest-examplecontroller](code/specs2/ExampleControllerSpec.scala)

and then test the trait:

@[scalatest-examplecontrollerspec](code/specs2/ExampleControllerSpec.scala)

## Unit Testing EssentialAction

Testing [`Action`](api/scala/index.html#play.api.mvc.Action) or [`Filter`](api/scala/index.html#play.api.mvc.Filter) can require to test an an [`EssentialAction`](api/scala/index.html#play.api.mvc.EssentialAction) ([[more information about what an EssentialAction is|HttpApi]])

For this, the test [`Helpers.call`](api/scala/index.html#play.api.test.Helpers@call) can be used like that:

@[scalatest-exampleessentialactionspec](code/specs2/ExampleEssentialActionSpec.scala)


> **Next:** [[Writing functional tests with ScalaTest|ScalaFunctionalTest]]
