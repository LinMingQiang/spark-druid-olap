
import SparkShim._

scalaVersion := "2.10.5"

crossScalaVersions := Seq("2.10.5", "2.11.6")

parallelExecution in Test := false

val nscalaVersion = "1.6.0"
val scalatestVersion = "2.2.4"
val httpclientVersion = "4.5"
val json4sVersion = "3.2.10"
val sparkdateTimeVersion = "0.0.2"
val scoptVersion = "3.3.0"
val druidVersion = "0.9.1"

val coreDependencies = Seq(
  "com.github.nscala-time" %% "nscala-time" % nscalaVersion,
  "org.apache.httpcomponents" % "httpclient" % httpclientVersion,
  // "org.json4s" %% "json4s-native" % json4sVersion,
  "org.json4s" %% "json4s-ext" % json4sVersion,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-smile" % "2.4.6",
  "com.fasterxml.jackson.jaxrs" % "jackson-jaxrs-smile-provider" % "2.4.6",
  "com.sparklinedata" %% "spark-datetime" % sparkdateTimeVersion,
  "com.github.scopt" %% "scopt" % scoptVersion,
  "org.scalatest" %% "scalatest" % scalatestVersion % "test"
)

val coreTestDependencies = Seq(
  "org.scalatest" %% "scalatest" % scalatestVersion % "test",
  "com.databricks" %% "spark-csv" % "1.1.0" % "test"
)
val druidTestEnvDependencies =Seq(
  "com.google.guava" % "guava" % guava_version force(),
  "org.apache.derby" % "derby" % derbyVersion force(),
  "org.apache.derby" % "derbyclient" % derbyVersion force(),
  "org.apache.derby" % "derbynet" % derbyVersion force(),
  "com.sun.jersey" % "jersey-servlet" % "1.17.1" % "test" force(),
  "com.metamx" %% "scala-util" % "1.11.6" exclude("log4j", "log4j") force(),
  "com.metamx" % "java-util" % "0.27.9" exclude("log4j", "log4j") force(),
  "io.druid" % "druid-server" % druidVersion,
  "io.druid" % "druid-services" % druidVersion,
  "org.apache.curator" % "curator-test" % "2.4.0" % "test" exclude("log4j", "log4j")  force(),
  "io.druid.extensions" % "druid-datasketches" % druidVersion
)

lazy val commonSettings = Seq(
  organization := "com.sparklinedata",

  version := "0.4.1",

  javaOptions := Seq("-Xms1g", "-Xmx3g",
    "-Duser.timezone=UTC",
    "-Dscalac.patmat.analysisBudget=512",
    "-XX:MaxPermSize=256M"),

  // Target Java 7
  scalacOptions += "-target:jvm-1.7",
  javacOptions in compile ++= Seq("-source", "1.7", "-target", "1.7"),

  scalacOptions := Seq("-feature", "-deprecation"),

  dependencyOverrides := Set(
    "org.apache.commons" % "commons-lang3" % "3.3.2"
  ),

  licenses := Seq("Apache License, Version 2.0" ->
    url("http://www.apache.org/licenses/LICENSE-2.0")
  ),

  homepage := Some(url("https://github.com/SparklineData/spark-druid-olap")),

  publishMavenStyle := true,

  publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (version.value.trim.endsWith("SNAPSHOT")) {
          Some("snapshots" at nexus + "content/repositories/snapshots")
      }
      else {
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
      }
  },

  publishArtifact in Test := false,

  pomIncludeRepository := { _ => false },

  test in assembly := {},

  useGpg := true,

  usePgpKeyHex("C922EB45"),

  pomExtra := (
    <scm>
      <url>https://github.com/SparklineData/spark-druid-olap.git</url>
      <connection>scm:git:git@github.com:SparklineData/spark-druid-olap.git</connection>
    </scm>
      <developers>
        <developer>
          <name>Harish Butani</name>
          <organization>SparklineData</organization>
          <organizationUrl>http://sparklinedata.com/</organizationUrl>
        </developer>
        <developer>
          <name>John Pullokkaran</name>
          <organization>SparklineData</organization>
          <organizationUrl>http://sparklinedata.com/</organizationUrl>
        </developer>

      </developers>),

  fork in Test := false,
  parallelExecution in Test := false
) ++ releaseSettings ++ Seq(
  ReleaseKeys.publishArtifactsAction := PgpKeys.publishSigned.value
)

lazy val spark1_6_1 = project.in(file("shims/spark-1.6.1"))
  .settings(
    libraryDependencies ++= spark161Dependencies
  )

lazy val spark1_6_2 = project.in(file("shims/spark-1.6.2"))
  .settings(
    libraryDependencies ++= spark162Dependencies
  )

lazy val sparkShimProject =
  if (sparkVersion == sparkVersion_161 ) spark1_6_1 else spark1_6_2

lazy val druidTestEnv = project.in(file("druidTestEnv"))
  .settings(
    libraryDependencies ++= (coreTestDependencies ++ druidTestEnvDependencies)
  )

lazy val root = project.in(file("."))
  .settings(commonSettings: _*)
  .settings(
    name := s"spl-accel$sparkNamExt",
    libraryDependencies ++= (sparkDependencies ++ coreDependencies ++ coreTestDependencies),
    assemblyOption in assembly :=
      (assemblyOption in assembly).value.copy(includeScala = false),
    assemblyExcludedJars in assembly := {
      val cp = (fullClasspath in assembly).value
      println(cp)
      cp filter {d => d.data.getName.startsWith("joda") || d.data.getName.startsWith("derby")}
    },
    publishArtifact in (Compile, packageBin) := false,
    publishArtifact in Test := true
  )
  .settings(addArtifact(artifact in (Compile, assembly), assembly).settings: _*)
  .dependsOn(
    sparkShimProject
  ).
  dependsOn(druidTestEnv % "test->test")

