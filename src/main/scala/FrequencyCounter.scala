import java.io.InputStream
import java.util.Properties

import edu.stanford.nlp.ling.CoreAnnotations.{PartOfSpeechAnnotation, SentencesAnnotation, TokensAnnotation}
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import org.apache.spark.input.PortableDataStream

import scala.collection.mutable
import scala.collection.JavaConverters._
import POSTags._
import nl.siegmann.epublib.domain.Book
import org.jsoup.Jsoup

object FrequencyCounter {

  def countFrecuenciesInSingleHTMLpage(file: String, dataStream: PortableDataStream): Map[String, Int] = {
    val pipeline: StanfordCoreNLP = getStanfordNLPParser
    val freqMap: mutable.Map[String, Int] = mutable.Map.empty
    updateWithWordsFromText(pipeline, getTextFromHTML(file, dataStream.open()), freqMap)
    freqMap.toMap
  }

  def countFrequenciesInEpubBook(file: String, book: Book): Map[String, Int] = {
    val pipeline: StanfordCoreNLP = getStanfordNLPParser

    val freqMap: mutable.Map[String, Int] = mutable.Map.empty

    val textInBook: Seq[String] = getTextInBook(book)
    textInBook.foreach { text =>
      updateWithWordsFromText(pipeline, text, freqMap)
    }

    freqMap.toMap
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
        if (posTags.contains(pos)) {
          freqMap.synchronized {
            val f = freqMap.getOrElse(pos, 0)
            freqMap.put(pos, f + 1)
          }
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
