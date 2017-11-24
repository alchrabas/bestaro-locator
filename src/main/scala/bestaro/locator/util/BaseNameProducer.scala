package bestaro.locator.util

import bestaro.locator.types
import bestaro.locator.types.{Gender, PartOfSpeech, Token}
import morfologik.stemming.polish.PolishStemmer

import scala.collection.JavaConverters._
import scala.collection.mutable

class BaseNameProducer {

  private val stemmer = new PolishStemmer

  private val _notFoundWords = mutable.HashMap[String, Int]()

  def strippedForStemming(original: String): String = {
    original.toLowerCase
      .replaceAll("[^0-9a-ząćęłńóśżź]", " ")
      .replaceAll("\\s+", " ")
      .trim
  }

  def maybeBestBaseToken(original: String): Option[Token] = {
    val strippedOriginal = strippedForStemming(original)
    var matchedStems = stemmer.lookup(strippedOriginal)
    if (matchedStems.isEmpty && strippedOriginal.nonEmpty && strippedOriginal.charAt(0).isUpper) {
      matchedStems = stemmer.lookup(strippedOriginal.capitalize)
    }

    if (matchedStems.isEmpty || isExcludedFromMorfologik(original)) {
      increaseNumberOfOccurrencesOfNotFoundWord(strippedOriginal)
      None
    } else {
      Some(
        matchedStems
          .asScala
          .map(tagInfo => types.Token(original,
            strippedOriginal,
            tagInfo.getStem.toString,
            InflectionUtil.getPartsOfSpeech(tagInfo),
            InflectionUtil.getGenders(tagInfo),
            0,
            flags = InflectionUtil.getFlags(tagInfo)
          ))
          .head
      )
    }
  }

  private def increaseNumberOfOccurrencesOfNotFoundWord(strippedOriginal: String) = {
    _notFoundWords.put(strippedOriginal, _notFoundWords.getOrElse(strippedOriginal, 0) + 1)
  }

  def notFoundWords: Map[String, Int] = {
    _notFoundWords.toMap
  }

  def getBestBaseToken(original: String): Token = {
    maybeBestBaseToken(original).getOrElse(Token(original,
      strippedForStemming(original),
      strippedForStemming(original),
      List(PartOfSpeech.OTHER),
      List(Gender.F), // because the most common "ulica" is feminine
      0
    ))
  }

  def getBestBaseName(original: String): String = {
    getBestBaseToken(original).stem
  }

  private def isExcludedFromMorfologik(word: String): Boolean = {
    Set("w", "i", "m", "o") contains word.toLowerCase
  }
}
