
lazy val common = Seq(
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8"),
  scalaVersion := "2.11.7",
  libraryDependencies ++= Seq(
    "com.chuusai" %% "shapeless" % "2.2.5",
    "com.google.protobuf" % "protobuf-java" % "2.6.1"
  ),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
)

lazy val root = project
  .in(file("."))
  .dependsOn(protos, macros)
  .aggregate(protos, macros)
  .settings(common:_*)
  .settings(
    mainClass in (Compile, run) := Some("com.joprice.protobuf.Main")
  )

lazy val macros = project
  .settings(common:_*)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value
    )
  )

lazy val protos = project
  .settings(common:_*)
  .settings(protobufSettings:_*)

