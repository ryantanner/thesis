name := "hello"

version := "1.0"

scalaVersion := "2.9.1"

squeryl := "org.squeryl" %% "squeryl" % "0.9.4"

mysqlDriver := "mysql" % "mysql-connector-java" % "5.1.10"

libraryDependencies ++= Seq(
  "net.databinder" %% "dispatch-http" % "0.8.6"
  )


