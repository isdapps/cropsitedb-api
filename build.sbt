name := """cropsitedb-api"""

version := "2.0.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc41",
  "org.mariadb.jdbc" % "mariadb-java-client" % "1.1.8",
  "org.agmip.ace"  % "ace-core"   % "2.0-SNAPSHOT",
  "org.apache.tika"% "tika-core"  % "1.6",
  "org.agmip"      % "dome"       % "1.4.7"
)
