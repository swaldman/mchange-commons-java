import sbt._

object MchangeCommonsJavaBuild extends Build {

  val nexus = "https://oss.sonatype.org/"
  val nexusSnapshots = nexus + "content/repositories/snapshots";
  val nexusReleases = nexus + "service/local/staging/deploy/maven2";

  val projectName = "mchange-commons-java"

  val mySettings = Seq( 
    Keys.organization := "com.mchange",
    Keys.name := projectName, 
    Keys.version := "0.2.5-SNAPSHOT", 
    Keys.autoScalaLibrary := false, // this is a pure Java library, don't depend on Scala
    Keys.crossPaths := false,       //don't include _<scala-version> in artifact names
    Keys.publishTo <<= Keys.version { 
      (v: String) => {
	if (v.trim.endsWith("SNAPSHOT"))
	  Some("snapshots" at nexusSnapshots )
	else
	  Some("releases"  at nexusReleases )
      }
    },
    Keys.resolvers += ("snapshots" at nexusSnapshots ),
    //Keys.scalacOptions += "-deprecation",
    Keys.pomExtra := pomExtraXml
  );

  val dependencies = Seq(
    //"com.typesafe" % "config" % "1.0.0"
  );

  override lazy val settings = super.settings ++ mySettings;

  lazy val mainProject = Project(
    id = projectName,
    base = file("."),
    settings = Project.defaultSettings ++ (Keys.libraryDependencies ++= dependencies)
  ); 

  val pomExtraXml = (
      <url>https://github.com/swaldman/mchange-commons-java</url>
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
       <url>git@github.com:swaldman/mchange-commons-java.git</url>
       <connection>scm:git:git@github.com:swaldman/mchange-commons-java.git</connection>
     </scm>
     <developers>
       <developer>
         <id>swaldman</id>
         <name>Steve Waldman</name>
         <email>swaldman@mchange.com</email>
       </developer>
     </developers>
  );
}

