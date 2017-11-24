package bestaro.locator.extractors

import bestaro.locator.{LocatorDatabase, types}
import bestaro.locator.types._
import bestaro.locator.util.BaseNameProducer

import scala.collection.mutable.ListBuffer

case class MatchedLocation(location: Location, position: Int, wordCount: Int)

case class MatchedFullLocation(fullLocation: FullLocation, position: Int, wordCount: Int)

object AbstractLocationExtractor {

  implicit class TokenListOps(private val tokens: List[Token]) extends AnyVal {
    def slidingPrefixedByEmptyTokens(size: Int): Iterator[List[Token]] = {
      (List.fill(size - 1)(EMPTY_TOKEN) ++ tokens).sliding(size)
    }
  }

  private val EMPTY_TOKEN = Token("", "", "", List(), List(), 0, flags = Set(Flag.EMPTY_TOKEN))
}

abstract class AbstractLocationExtractor(locatorDatabase: LocatorDatabase, memoryCache: Boolean) {

  import AbstractLocationExtractor._

  private val NOUN_PRECEDING_NAME_SCORE = 11
  private val CAPITALIZED_WORD_SCORE = 5
  private val PRECEDED_BY_NOUN_THAT_SUGGESTS_LOCATION_SCORE = 8
  private val PRECEDED_BY_LOC_SPECIFIC_PREPOSITION_SCORE = 5
  private val NAME_ON_IGNORE_LIST_SCORE = -20

  protected val baseNameProducer = new BaseNameProducer

  protected val townNamesExtractor = new InflectedTownNamesExtractor(locatorDatabase, memoryCache)

  protected var onIgnoreListPredicate: (Token, FullLocation) => Boolean = (_, _) => false

  def setOnIgnoreListPredicate(predicate: (Token, FullLocation) => Boolean): Unit = {
    onIgnoreListPredicate = predicate
  }

  def extractLocation(tokens: List[String], alreadyKnownLocation: FullLocation): (List[Token], List[MatchedFullLocation]) = {

    println("########################")
    var stemmedTokens = tokens.map { tokenText =>
      val strippedTokenText = baseNameProducer.strippedForStemming(tokenText)
      val stemmedTokenText = baseNameProducer.getBestBaseName(tokenText)
      if (isNounThatSuggestsLocationName(strippedTokenText,
        stemmedTokenText)) {
        newNounToken(tokenText, strippedTokenText, stemmedTokenText)
      } else {
        evaluateMostAccurateBaseName(tokenText)
      }
    }.map(token => {
      if (onIgnoreListPredicate(token, alreadyKnownLocation)) {
        token.withAlteredPlacenessScore(NAME_ON_IGNORE_LIST_SCORE)
      } else {
        token
      }
    })

    if (stemmedTokens.nonEmpty) {
      stemmedTokens = updateTokenEvaluationUsingContext(stemmedTokens)
    }
    val foundLocationNames = townNamesExtractor.findLocationNamesFromDatabase(stemmedTokens.map(_.stripped),
      alreadyKnownLocation.voivodeship)
    if (stemmedTokens.nonEmpty) {
      println(">>> " + foundLocationNames)
      stemmedTokens = stemmedTokens.zipWithIndex.map { case (token, position) =>
        val tokenMatches = foundLocationNames.exists(townName =>
          (townName.initialPos until (townName.initialPos + townName.wordCount)) contains position)
        if (tokenMatches) {
          token.withAlteredPlacenessScore(5)
        } else {
          token
        }
      }
    }

    val (mutableTokens, matchedStreets) = specificExtract(alreadyKnownLocation, stemmedTokens, foundLocationNames)
    (mutableTokens.toList, matchedStreets.toList)
  }

  private def newNounToken(tokenText: String, strippedTokenText: String, stemmedTokenText: String) = {
    val gender = getGenderOfNounPrecedingName(strippedTokenText, stemmedTokenText)
    types.Token(tokenText,
      strippedTokenText,
      stemmedTokenText,
      List(PartOfSpeech.NOUN),
      List(gender),
      NOUN_PRECEDING_NAME_SCORE,
      flags = createFlagsForNounPrecedingLocationName(tokenText))
  }

  private def createFlagsForNounPrecedingLocationName(tokenText: String) = {
    Set(Flag.NOUN_PRECEDING_LOCATION_NAME) ++
      (if (tokenText.endsWith(".")) {
        Set(Flag.PUNCTUATED_WORD)
      } else {
        Set()
      })
  }

  protected def specificExtract(alreadyKnownLocation: FullLocation, stemmedTokens: List[Token],
                                foundLocationNames: Seq[MatchedInflectedLocation]
                               ): (ListBuffer[Token], ListBuffer[MatchedFullLocation])


  private def updateTokenEvaluationUsingContext(tokens: List[Token]): List[Token] = {
    tokens.slidingPrefixedByEmptyTokens(2).map { case List(nameTrait, toReturn) =>
      if (isNounThatSuggestsName(nameTrait)) {
        toReturn.withAlteredPlacenessScore(PRECEDED_BY_NOUN_THAT_SUGGESTS_LOCATION_SCORE)
      } else {
        toReturn
      }
    }.toList.slidingPrefixedByEmptyTokens(2).map { case List(preposition, toReturn) =>
      if (isLocationSpecificPreposition(preposition)) {
        toReturn.withAlteredPlacenessScore(PRECEDED_BY_LOC_SPECIFIC_PREPOSITION_SCORE)
      } else {
        toReturn
      }
    }.toList.slidingPrefixedByEmptyTokens(2).map { case List(previous, toReturn) =>
      if (isCapitalized(toReturn.original)
        && !previous.isEndOfSentence
        && !isUpperCase(toReturn.original)
        && !previous.flags.contains(Flag.EMPTY_TOKEN)) {
        toReturn.withAlteredPlacenessScore(CAPITALIZED_WORD_SCORE)
      } else {
        toReturn
      }
    }.toList.slidingPrefixedByEmptyTokens(3).map { case List(preposition, nameTrait, toReturn) =>
      val isPrepositionFollowedByKind = isLocationSpecificPreposition(preposition) && isNounThatSuggestsName(nameTrait)
      if (isPrepositionFollowedByKind) {
        toReturn.withAlteredPlacenessScore(PRECEDED_BY_LOC_SPECIFIC_PREPOSITION_SCORE)
      } else {
        toReturn
      }
    }.toList
  }

  private def isNounThatSuggestsName(token: Token): Boolean = {
    isNounThatSuggestsLocationName(token.stripped, token.stem)
  }

  private def isNounThatSuggestsLocationName(stripped: String, stem: String): Boolean = {
    (Set("ul", "pl", "os", "al") contains stripped) ||
      (Set("plac", "ulica", "osiedle", "aleja") contains stem)
  }

  private val strippedNounPrecedingNameToGender = Map(
    "ul" -> Gender.F,
    "pl" -> Gender.M,
    "os" -> Gender.N,
    "al" -> Gender.F
  )

  private val stemmedNounPrecedingNameToGender = Map(
    "plac" -> Gender.M,
    "ulica" -> Gender.F,
    "osiedle" -> Gender.N,
    "aleja" -> Gender.F
  )

  private def getGenderOfNounPrecedingName(stripped: String, stemmed: String): Gender = {
    strippedNounPrecedingNameToGender.getOrElse(stripped, stemmedNounPrecedingNameToGender(stemmed))
  }

  private def isLocationSpecificPreposition(token: Token): Boolean = {
    (Set("w", "we", "nad", "na", "przy") contains token.stripped) ||
      (Set("okolica", "pobliże") contains token.stem)
  }

  private def evaluateMostAccurateBaseName(original: String): Token = {
    val strippedText = baseNameProducer.strippedForStemming(original)
    baseNameProducer.maybeBestBaseToken(original).map(_.copy(placenessScore = 1)).getOrElse(
      Token(original,
        strippedText, strippedText,
        List(PartOfSpeech.OTHER),
        List(Gender.F), // because the most common "ulica" is feminine
        0)
    )
  }

  private def isCapitalized(original: String): Boolean = {
    !original.isEmpty && original(0).isUpper
  }

  def isUpperCase(original: String): Boolean = {
    original.toUpperCase == original
  }
}
