val nexus = "https://oss.sonatype.org/"
val nexusSnapshots = nexus + "content/repositories/snapshots";
val nexusStaging = nexus + "service/local/staging/deploy/maven2";

organization := "com.mchange"

name := "mchange-commons-java"

version := "0.2.19"

autoScalaLibrary := false // this is a pure Java library, don't depend on Scala

crossPaths := false //don't include _<scala-version> in artifact names

javacOptions in (Compile, compile) ++= Seq("-source","1.6","-target","1.6"/*,"-Xlint:deprecation","-Xlint:unchecked"*/)

//javacOptions in (Compile, compile) ++= Seq("-Xlint:all")

javacOptions in (Compile, doc) ++= Seq("-source","1.6")

javacOptions in (doc) += "-Xdoclint:none"

libraryDependencies ++= Seq(
    "com.typesafe"             % "config"          % "1.3.0"   % "compile,optional",
    "log4j"                    % "log4j"           % "1.2.14+" % "compile,optional",
    "org.apache.logging.log4j" % "log4j-api"       % "2.7"     % "compile,optional",
    "org.apache.logging.log4j" % "log4j-core"      % "2.7"     % "compile,optional",
    "org.slf4j"                % "slf4j-api"       % "1.7.5+"  % "compile,optional",
    "junit"                    % "junit"           % "4.1+"    % "test",
    "ch.qos.logback"           % "logback-classic" % "1.1.2"   % "test",
    "com.novocode"             % "junit-interface" % "0.10-M3" % "test" 
);


publishTo := {
  if (isSnapshot.value) Some("snapshots" at nexusSnapshots ) else Some("releases"  at nexusStaging )
}

resolvers += ("snapshots" at nexusSnapshots )

/*
 * For some mysterious reasons, forking to test and setting java options on
 * the forked JVM is misbehaving. Workaround is to not fork at all, but
 * test with commands like
 *    $ export _JAVA_OPTIONS="-ea -Djava.util.logging.config.file=./src/test/resources/logging.properties"; sbt clean test; unset _JAVA_OPTIONS
 */ 
//scalacOptions += "-deprecation"
//fork in Test := true
//javaOptions in test += "-ea"

logLevel in Test := Level.Debug
testOptions += Tests.Argument(TestFrameworks.JUnit, "-a -v")

pomExtra := pomExtraForProjectName( name.value )

def pomExtraForProjectName( projectName : String ) = {
    <url>https://github.com/swaldman/{projectName}</url>
    <licenses>
      <license>
        <name>GNU Lesser General Public License, Version 2.1</name>
        <url>http://www.gnu.org/licenses/lgpl-2.1.html</url>
        <distribution>repo</distribution>
      </license>
      <license>
        <name>Eclipse Public License, Version 1.0</name>
        <url>http://www.eclipse.org/org/documents/epl-v10.html</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:swaldman/{projectName}.git</url>
      <connection>scm:git:git@github.com:swaldman/{projectName}</connection>
    </scm>
    <developers>
      <developer>
        <id>swaldman</id>
        <name>Steve Waldman</name>
        <email>swaldman@mchange.com</email>
      </developer>
    </developers>
}


