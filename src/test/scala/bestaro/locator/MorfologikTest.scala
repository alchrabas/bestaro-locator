package bestaro.locator

import morfologik.stemming.WordData
import morfologik.stemming.polish.PolishStemmer
import org.scalatest.FunSpec

import scala.collection.JavaConverters._

class MorfologikTest extends FunSpec {

  private val stemmer = new PolishStemmer

  describe("Morfologik") {
    it("should correctly get stem of singular words") {
      assert(getOnlyWordStem("zamek") == "zamek")
      assert(getOnlyWordStem("dworze") == "dwór")
      assert(getOnlyWordStem("budynkiem") == "budynek")
      assert(getOnlyWordStem("Andrzejkowi") == "Andrzejek")
    }

    it("should correctly get stem of plural words") {
      assert(getOnlyWordStem("koszom") == "kosz")
      assert(getOnlyWordStem("łyżki") == "łyżka")
      assert(getOnlyWordStem("antylop") == "antylopa")
    }

    it("should correctly get nominative version of adjectives") {
      assert(getAllWordStems("najczerwieńszych") == List("czerwony"))
      assert(getAllWordStems("czarni") == List("czarny"))
      assert(getAllWordStems("czerwonej") == List("czerwona", "czerwony"))
      assert(getAllWordStems("zdrową") == List("zdrowy"))
    }
  }

  private def getAllWordStems(word: String): List[String] = {
    stemmer.lookup(word).asScala.map(_.getStem.toString).toList
  }

  private def getOnlyWordStem(word: String): String = {
    onlyElement(stemmer.lookup(word)).getStem.toString
  }

  private def onlyElement(oneElementList: java.util.List[WordData]): WordData = {
    if (oneElementList.size != 1) {
      throw new IllegalArgumentException(s"List has ${oneElementList.size} elements, but it should have a single one")
    }
    oneElementList.get(0)
  }
}
