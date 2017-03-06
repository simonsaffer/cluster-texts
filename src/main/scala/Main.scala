import java.io.PrintWriter

import POSTags._
import nl.siegmann.epublib.epub.EpubReader
import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.{SparkConf, SparkContext}

object Main {

  val NUMERIC_CONDITIONER = 100

  def convertToVector(freqMap: Map[String, Int]) = {

    val totFreq = freqMap.foldLeft(0) {
      case (tot, (_, freq)) => tot + freq
    }.toDouble

    val indicesWithNormalizedFrequencies = freqMap.toSeq.map {
      case (pos, freq) => {
        (posToIndexMap(pos), (freq * NUMERIC_CONDITIONER).toDouble / totFreq)
      }
    }

    Vectors.sparse(posTags.size, indicesWithNormalizedFrequencies)
  }

  def main(args: Array[String]): Unit = {

    val conf = new SparkConf().setAppName("Analisis de gramatica").setMaster("local[4]")
    val sc = new SparkContext(conf)
    val allBooks = sc.binaryFiles("src/main/resources/x.html")

    val allPOSFeatureVectorsAndBookTitles = allBooks.map {
      case (file, dataStream) if file.endsWith("epub") => {
        val epubReader = new EpubReader()
        val book = epubReader.readEpub(dataStream.open())

        val frecuencies = FrequencyCounter.countFrequenciesInEpubBook(file, book)

        (book.getTitle, convertToVector(frecuencies))
      }
      case (file, dataStream) if file.endsWith("html")=> {

        val frequencies = FrequencyCounter.countFrecuenciesInSingleHTMLpage(file, dataStream)

        (file, convertToVector(frequencies))
      }
    }

    allPOSFeatureVectorsAndBookTitles.cache()

    val allPOSFeatureVectors = allPOSFeatureVectorsAndBookTitles.map(_._2)

    val numClusters = 4
    val numIterations = 100
    val clusters = KMeans.train(allPOSFeatureVectors, numClusters, numIterations)

    val WSSSE = clusters.computeCost(allPOSFeatureVectors)
    println("Within Set Sum of Squared Errors = " + WSSSE)

    val pw = new PrintWriter("text-groups.csv")

    pw.print("Title,")
    pw.print("Group,")
    pw.println(posTags.mkString(","))

    allPOSFeatureVectorsAndBookTitles.collect.foreach {
      case (title, vector) =>
        val cluster = clusters.predict(vector)

        val row: Array[String] = Array(title.replace(",", ""), cluster.toString) ++ vector.toArray.map(d => Math.round(d).toInt.toString)
        pw.println(row.mkString(","))
    }

    pw.close()
    sc.stop()

  }

}
