name := "cluster-texts"

version := "1.0"

scalaVersion := "2.11.1"

libraryDependencies += "org.apache.spark" % "spark-core_2.11" % "2.1.0"

libraryDependencies += "com.typesafe.play" %% "play-ws" % "2.4.8"

libraryDependencies += "org.jsoup" % "jsoup" % "1.10.2"

libraryDependencies += "org.apache.spark" % "spark-mllib_2.11" % "2.1.0"

val STANFORD_NLP_VERSION = "3.6.0"
libraryDependencies += "edu.stanford.nlp" % "stanford-parser" % STANFORD_NLP_VERSION
libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % STANFORD_NLP_VERSION
libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % STANFORD_NLP_VERSION classifier "models"
libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % STANFORD_NLP_VERSION classifier "models-spanish"
