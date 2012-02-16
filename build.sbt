name := "scala-async"

version := "1.0"

scalaVersion := "2.9.1"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.typesafe.akka" % "akka-actor" % "2.0-RC1"

autoCompilerPlugins := true

libraryDependencies <<= (scalaVersion, libraryDependencies) { (ver, deps) =>
    deps :+ compilerPlugin("org.scala-lang.plugins" % "continuations" % ver)
}

libraryDependencies <+= scalaVersion( "org.scala-lang" % "scala-swing" % _ )

scalacOptions += "-P:continuations:enable"

scalacOptions += "-deprecation"
