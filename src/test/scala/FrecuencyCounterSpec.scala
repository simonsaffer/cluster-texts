import org.apache.spark.{SparkConf, SparkContext}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class FrecuencyCounterSpec extends Specification {

  "FC" should {
    "correctly count frequencies" in {

      val conf = new SparkConf().setAppName("Analisis de gramatica").setMaster("local[4]")
      val sc = new SparkContext(conf)
      val page = sc.binaryFiles("src/test/resources/x.html")

      val freqMaps = page.map {
        case (file, dataStream) if file.endsWith("html") => {

          FrequencyCounter.countFrecuenciesInSingleHTMLpage(file, dataStream)


        }
      }

      val freqMap = freqMaps.collect().head

      sc.stop()

      freqMap shouldEqual Map(
        "vmii000" -> 2,
        "aq0000" -> 2,
        "nc0s000" -> 3,
        "vmip000" -> 1,
        "vsii000" -> 1,
        "vmn0000" -> 1,
        "vmis000" -> 1
      )
    }
  }
}