
lazy val commonSettings = Seq(
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8"),
  scalaVersion := "2.11.7",
  libraryDependencies ++= Seq(
    "com.chuusai" %% "shapeless" % "2.2.5",
    "com.google.protobuf" % "protobuf-java" % "2.6.1"
  ),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
)

lazy val macros = project
  .settings(commonSettings:_*)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value
    )
  )

lazy val example = project
  .settings(commonSettings:_*)
  .dependsOn(protos, macros)
  .settings(
    mainClass in (Compile, run) := Some("com.joprice.protobuf.Main"),
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "2.2.6" % "test"
    )
  )

// protos need to be compiled in another compilation run, or in a jar, in 
// order to be used by the macros
lazy val protos = project
  .settings(commonSettings:_*)
  .settings(protobufSettings:_*)

lazy val akka = project
  .dependsOn(macros, protos)
  .settings(commonSettings:_*)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "2.2.6" % "test",
      "com.typesafe.akka" %% "akka-actor" % "2.4.1"
    )
  )

