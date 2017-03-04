import java.io.{InputStream, PrintWriter}
import java.util.Properties

import edu.stanford.nlp.ling.CoreAnnotations._
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.epub.EpubReader
import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.{SparkConf, SparkContext}
import org.jsoup.Jsoup

import scala.collection.JavaConverters._
import scala.collection.mutable

object Main {

  val NUMERIC_CONDITIONER = 100

  // http://nlp.stanford.edu/software/spanish-faq.shtml
  val posTags = Array(
    "ao0000", "aq0000", // Adjectives
    "cc", "cs", // Conjunctions
    "da0000", "dd0000", "de0000", "di0000", "dn0000", "dp0000", "dt0000", // Determiners
    "f0", "faa", "fat", "fc", "fd", "fe", "fg", "fh", "fia", "fit", "fp", "fpa", "fpt", "fs", "ft", "fx", "fz", // Punctuation
    "i", // Interjections
    "nc00000", "nc0n000", "nc0p000", "nc0s000", "np00000", // Nouns
    "p0000000", "pd000000", "pe000000", "pi000000", "pn000000", "pp000000", "pr000000", "pt000000", "px000000", // Pronouns
    "rg", "rn", // Adverbs
    "sp000", // Prepositions
    "vag0000", "vaic000", "vaif000", "vaii000", "vaip000", "vais000", "vam0000", "van0000", "vap0000", "vasi000", // Verbs ...
    "vasp000", "vmg0000", "vmic000", "vmif000", "vmii000", "vmip000", "vmis000", "vmm0000", "vmn0000", "vmp0000", //...
    "vmsi000", "vmsp000", "vsg0000", "vsic000", "vsif000", "vsii000", "vsip000", "vsis000", "vsm0000", "vsn0000", // ...
    "vsp0000", "vssf000", "vssi000", "vssp000",
    "w", // Dates
    "z0", "zm", "zu", // Numerals
    "word", // Other
    "359000"
    //"vmmp000", "va00000", "vsmp000", "359000", "aqs000", "vmim000", "vmi0000", "vmms000", "vm0p000", "ap0000", "zp", "vq00000", "vm00000", "do0000", "vs00000" // Not documented POS
  )
  val posToIndexMap = posTags.zipWithIndex.toMap

  def convertToVector(freqMap: mutable.Map[String, Int]) = {

    val totFreq = freqMap.foldLeft(0) {
      case (tot, (_, freq)) => tot + freq
    }.toDouble

    val indicesWithNormalizedFrequencies = freqMap.toSeq.map {
      case (pos, freq) => {
        (posToIndexMap(pos), (freq * NUMERIC_CONDITIONER).toDouble / totFreq)
      }
    }

    Vectors.sparse(posTags.length, indicesWithNormalizedFrequencies)
  }

  def main(args: Array[String]): Unit = {

    val conf = new SparkConf().setAppName("Analisis de gramatica").setMaster("local[4]")
    val sc = new SparkContext(conf)
    val allBooks = sc.binaryFiles("src/main/resources/*")

    val allPOSFeatureVectorsAndBookTitles = allBooks.map {
      case (file, dataStream) if file.endsWith("epub") => {
        val epubReader = new EpubReader()
        val book = epubReader.readEpub(dataStream.open())

        val pipeline: StanfordCoreNLP = getStanfordNLPParser

        val freqMap: mutable.Map[String, Int] = mutable.Map.empty

        val textInBook: Seq[String] = getTextInBook(book)
        textInBook.foreach { text =>
          updateWithWordsFromText(pipeline, text, freqMap)
        }

        val freqVector = convertToVector(freqMap)
        (book.getTitle, freqVector)
      }
      case (file, dataStream) if file.endsWith("html")=> {
        val pipeline: StanfordCoreNLP = getStanfordNLPParser
        val freqMap: mutable.Map[String, Int] = mutable.Map.empty
        updateWithWordsFromText(pipeline, getTextFromHTML(file, dataStream.open()), freqMap)
        val freqVector = convertToVector(freqMap)
        (file, freqVector)
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

  private def getStanfordNLPParser = {
    val props = new Properties()
    props.setProperty("annotators", "tokenize, ssplit, pos")
    props.setProperty("tokenize.language", "es")
    props.setProperty("language", "spanish")
    props.setProperty("pos.model", "edu/stanford/nlp/models/pos-tagger/spanish/spanish-distsim.tagger")
    val pipeline = new StanfordCoreNLP(props)
    pipeline
  }

  private def updateWithWordsFromText(pipeline: StanfordCoreNLP, text: String, freqMap: mutable.Map[String, Int]) = {
    val doc = new Annotation(text)
    pipeline.annotate(doc)

    val sentences = doc.get(classOf[SentencesAnnotation]).asScala

    sentences.foreach(sentence => {
      sentence.get(classOf[TokensAnnotation]).asScala.foreach(token => {
        val pos = token.get(classOf[PartOfSpeechAnnotation])
        if (!posToIndexMap.get(pos).isDefined) println(s"Token undefined: ${token.toString}, pos: $pos")
        freqMap.synchronized {
          val f = freqMap.getOrElse(pos, 0)
          freqMap.put(pos, f + 1)
        }
      })
    })
  }

  private def getTextInBook(book: Book): Seq[String] = {
    book.getContents.listIterator.asScala.toSeq
      .filterNot(resource => resource.getId.equalsIgnoreCase("cover") || resource.getId.equalsIgnoreCase("tiltlepage"))
      .map(resource => {
      val parsedPage = Jsoup.parse(resource.getInputStream, resource.getInputEncoding, resource.getHref)
      parsedPage.body().text()
    })
  }

  private def getTextFromHTML(file: String, inputStream: InputStream): String = {
    val parsedPage = Jsoup.parse(inputStream, "UTF-8", file)
    parsedPage.body().text()
  }

}
