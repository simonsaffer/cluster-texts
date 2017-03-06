import org.apache.spark.mllib.linalg.Vectors
import org.specs2.mutable.Specification
import POSTags.{posTags, posToIndexMap}
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MainSpec extends Specification {

  "Main" should {

    "Convert frequency mac to a vector correctly" in {
      val freqMap = Map(
        "vmii000" -> 2,
        "aq0000" -> 2,
        "nc0s000" -> 3,
        "vmip000" -> 1,
        "vsii000" -> 1,
        "vmn0000" -> 1,
        "vmis000" -> 1
      )
      val result = Main.convertToVector(freqMap)

      val tot = freqMap.map(_._2).sum.toDouble
      val indexAndFreq = freqMap.map(t => (posToIndexMap(t._1), (t._2*100)/tot)).toSeq
      result shouldEqual Vectors.sparse(posTags.size, indexAndFreq)
    }

  }

}
