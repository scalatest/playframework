import java.util.jar.JarFile
import play.console.Colors
import play.core.server.ServerWithStop
import sbt._
import sbt.Keys._
import play.Keys._
import play.core.{ SBTDocHandler, SBTLink, PlayVersion }
import play.PlaySourceGenerators._
import DocValidation._

object ApplicationBuild extends Build {

  /*
   * Circular dependency support.
   *
   * We want to build docs with the main build, including in pull requests, etc.  Hence, we can't have any circular
   * dependencies where the documentation code snippets depend on code external to Play, that itself depends on Play,
   * in case a new change in Play breaks the external code.
   *
   * To address this, we have multiple modes that this build can run in, controlled by an external.modules system
   * property.
   *
   * If this property is not set, or is none, we only compile/test the code snippets that doesn't depend on external
   * modules.
   *
   * If it is all, we compile/test all code snippets.
   *
   * If it is a comma separated list of projects, we compile/test that comma separated list of projects.
   *
   * To add a new project, let's call it foo, add a new entry to the map below with that projects dependencies keyed
   * with "foo".  Then place all the code snippets that use that external module in a directory called "code-foo".
   */

  val externalPlayModules: Map[String, Seq[Setting[_]]] = Map(
  )

  val enabledExternalPlayModules = Option(System.getProperty("external.modules"))

  val (externalPlayModuleSettings, codeFilter): (Seq[Setting[_]], FileFilter) = enabledExternalPlayModules match {
    case None | Some("none") => (Nil, new ExactFilter("code"))
    case Some("all") => (externalPlayModules.toSeq.flatMap(_._2),
      new ExactFilter("code") || FileFilter.globFilter("code-*"))
    case Some(explicit) =>
      val enabled = explicit.split(",")
      (enabled.flatMap(e => externalPlayModules.get(e).getOrElse(Nil)),
        enabled.foldLeft[FileFilter](new ExactFilter("code")) { (filter, e) => filter || new ExactFilter("code-" + e) })
  }

  lazy val main = Project("Play-Documentation", file(".")).settings(
    version := PlayVersion.current,
    scalaVersion := PlayVersion.scalaVersion,
    libraryDependencies ++= Seq(
      component("play") % "test",
      component("play-test") % "test",
      component("play-java") % "test",
      component("play-cache") % "test",
      component("filters-helpers") % "test",
      "org.mockito" % "mockito-core" % "1.9.5" % "test",
      "org.scalatestplus" %% "play" % "0.8.0-SNAP5" % "test" exclude("com.typesafe.play", "play-test_2.10"),
      component("play-docs")
    ),

    javaManualSourceDirectories <<= (baseDirectory)(base => (base / "manual" / "javaGuide" ** codeFilter).get),
    scalaManualSourceDirectories <<= (baseDirectory)(base => (base / "manual" / "scalaGuide" ** codeFilter).get),

    unmanagedSourceDirectories in Test <++= javaManualSourceDirectories,
    unmanagedSourceDirectories in Test <++= scalaManualSourceDirectories,
    unmanagedSourceDirectories in Test <++= (baseDirectory)(base => (base / "manual" / "detailedTopics" ** codeFilter).get),

    unmanagedResourceDirectories in Test <++= javaManualSourceDirectories,
    unmanagedResourceDirectories in Test <++= scalaManualSourceDirectories,
    unmanagedResourceDirectories in Test <++= (baseDirectory)(base => (base / "manual" / "detailedTopics" ** codeFilter).get),

    parallelExecution in Test := false,

    (compile in Test) <<= Enhancement.enhanceJavaClasses,

    javacOptions ++= Seq("-g", "-Xlint:deprecation"),

    // Need to ensure that templates in the Java docs get Java imports, and in the Scala docs get Scala imports
    sourceGenerators in Test <+= (state, javaManualSourceDirectories, sourceManaged in Test, templatesTypes) map { (s, ds, g, t) =>
      ScalaTemplates(s, ds, g, t, defaultTemplatesImport ++ defaultJavaTemplatesImport)
    },
    sourceGenerators in Test <+= (state, scalaManualSourceDirectories, sourceManaged in Test, templatesTypes) map { (s, ds, g, t) =>
      ScalaTemplates(s, ds, g, t, defaultTemplatesImport ++ defaultScalaTemplatesImport)
    },

    sourceGenerators in Test <+= (state, javaManualSourceDirectories, sourceManaged in Test) map  { (s, ds, g) =>
      RouteFiles(s, ds, g, Seq("play.libs.F"), true, true)
    },
    sourceGenerators in Test <+= (state, scalaManualSourceDirectories, sourceManaged in Test) map  { (s, ds, g) =>
      RouteFiles(s, ds, g, Seq(), true, true)
    },

    templatesTypes := Map(
      "html" -> "play.api.templates.HtmlFormat"
    ),

    run <<= docsRunSetting,

    generateMarkdownReport <<= GenerateMarkdownReportTask,
    validateDocs <<= ValidateDocsTask,
    validateExternalLinks <<= ValidateExternalLinksTask,

    testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "sequential", "true", "junitxml", "console"),
    testOptions in Test += Tests.Argument(TestFrameworks.JUnit, "--ignore-runners=org.specs2.runner.JUnitRunner"),
    testListeners <<= (target, streams).map((t, s) => Seq(new eu.henkelmann.sbt.JUnitXmlTestsListener(t.getAbsolutePath, s.log)))

  ).settings(externalPlayModuleSettings:_*)

  // Run a documentation server
  val docsRunSetting: Project.Initialize[InputTask[Unit]] = inputTask { (argsTask: TaskKey[Seq[String]]) =>
    (argsTask, state) map runServer
  }

  private def runServer(args: Seq[String], state: State) = {
    val extracted = Project.extract(state)

    val port = args.headOption.map(_.toInt).getOrElse(9000)

    // Get classloader
    val sbtLoader = this.getClass.getClassLoader
    Project.runTask(dependencyClasspath in Test, state).get._2.toEither.right.map { classpath: Seq[Attributed[File]] =>
      val classloader = new java.net.URLClassLoader(classpath.map(_.data.toURI.toURL).toArray, null /* important here, don't depend of the sbt classLoader! */) {
        val sharedClasses = Seq(
          classOf[play.core.SBTLink].getName,
          classOf[play.core.SBTDocHandler].getName,
          classOf[play.core.server.ServerWithStop].getName,
          classOf[play.api.UsefulException].getName,
          classOf[play.api.PlayException].getName,
          classOf[play.api.PlayException.InterestingLines].getName,
          classOf[play.api.PlayException.RichDescription].getName,
          classOf[play.api.PlayException.ExceptionSource].getName,
          classOf[play.api.PlayException.ExceptionAttachment].getName)

        override def loadClass(name: String): Class[_] = {
          if (sharedClasses.contains(name)) {
            sbtLoader.loadClass(name)
          } else {
            super.loadClass(name)
          }
        }
      }

      val projectPath = extracted.get(baseDirectory)
      val docsJarFile = {
        val f = classpath.map(_.data).filter(_.getName.startsWith("play-docs")).head
        new JarFile(f)
      }
      val sbtDocHandler = {
        val docHandlerFactoryClass = classloader.loadClass("play.docs.SBTDocHandlerFactory")
        val fromDirectoryMethod = docHandlerFactoryClass.getMethod("fromDirectoryAndJar", classOf[java.io.File], classOf[JarFile], classOf[String])
        fromDirectoryMethod.invoke(null, projectPath, docsJarFile, "play/docs/content")
      }

      val clazz = classloader.loadClass("play.docs.DocumentationServer")
      val constructor = clazz.getConstructor(classOf[File], classOf[SBTDocHandler], classOf[java.lang.Integer])
      val server = constructor.newInstance(projectPath, sbtDocHandler, new java.lang.Integer(port)).asInstanceOf[ServerWithStop]

      println()
      println(Colors.green("Documentation server started, you can now view the docs by going to http://localhost:" + port))
      println()

      waitForKey()

      server.stop()
    }

    state
  }

  private lazy val consoleReader = {
    val cr = new jline.console.ConsoleReader
    // Because jline, whenever you create a new console reader, turns echo off. Stupid thing.
    cr.getTerminal.setEchoEnabled(true)
    cr
  }

  private def waitForKey() = {
    consoleReader.getTerminal.setEchoEnabled(false)
    def waitEOF() {
      consoleReader.readCharacter() match {
        case 4 => // STOP
        case 11 => consoleReader.clearScreen(); waitEOF()
        case 10 => println(); waitEOF()
        case _ => waitEOF()
      }

    }
    waitEOF()
    consoleReader.getTerminal.setEchoEnabled(true)
  }

  lazy val javaManualSourceDirectories = SettingKey[Seq[File]]("java-manual-source-directories")
  lazy val scalaManualSourceDirectories = SettingKey[Seq[File]]("scala-manual-source-directories")

}
