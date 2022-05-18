package bestaro.locator.util

import bestaro.locator.types.{Flag, Gender, PartOfSpeech}
import morfologik.stemming.WordData
import morfologik.stemming.polish.PolishStemmer
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec

class InflectionUtilTest extends AnyFunSpec with MockFactory {
  /*
    * It's impossible to instantiate WordData outside of PolishStemmer, so it's necessary to depend on lookup method.
    */
  val polishStemmer = new PolishStemmer()

  describe("Gender function") {

    it("should extract single gender from adjective 'czarny'") {
      assert(InflectionUtil.getGenders(lookupFirstResult("czarny")).toSet == List(Gender.M).toSet)
    }

    it("should extract all genders from adjective 'olkuskie'") {
      assert(InflectionUtil.getGenders(lookupFirstResult("olkuskie")).toSet == List(Gender.M, Gender.F, Gender.N).toSet)
    }
  }

  describe("Part Of Speech function") {

    it("should extract part of speech from adjective 'uśmiechnięty'") {
      assert(InflectionUtil.getPartsOfSpeech(lookupFirstResult("uśmiechnięty")) == List(PartOfSpeech.ADJECTIVE))
    }

    it("should extract part of speech from adjective 'olkuskie'") {
      assert(InflectionUtil.getPartsOfSpeech(lookupFirstResult("olkuskie")) == List(PartOfSpeech.ADJECTIVE))
    }

    it("should extract part of speech from noun 'krawaty'") {
      assert(InflectionUtil.getPartsOfSpeech(lookupFirstResult("krawaty")) == List(PartOfSpeech.NOUN))
    }

    it("should extract part of speech from verb 'pracuję'") {
      assert(InflectionUtil.getPartsOfSpeech(lookupFirstResult("pracuję")) == List(PartOfSpeech.VERB))
    }
  }

  describe("Flags function") {
    it("correctly notice that 'św' requires a trailing dot") {
      assert(InflectionUtil.getFlags(lookupFirstResult("św")) == Set(Flag.PUNCTUATED_WORD))
    }
  }

  private def lookupFirstResult(word: String): WordData = {
    polishStemmer.lookup(word).get(0)
  }
}
