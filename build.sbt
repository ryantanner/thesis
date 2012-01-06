name := "thesis"

version := "1.0"

scalaVersion := "2.9.1"

libraryDependencies ++= Seq(
  "net.databinder" %% "dispatch-http" % "0.8.6"
  )

libraryDependencies ++= Seq(
    "org.squeryl" %% "squeryl" % "0.9.4"
)

libraryDependencies ++= Seq(
    "postgresql" % "postgresql" % "8.4-701.jdbc4"
)


