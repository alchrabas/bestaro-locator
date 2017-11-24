package bestaro.locator.extractors

import bestaro.locator.types.{LocationType, Token}

case class MultiWordName(tokens: List[Token], startIndex: Int,
                         locType: Option[LocationType] = None) {
  def pushWord(token: Token): MultiWordName = {
    copy(tokens = tokens :+ token)
  }

  def sumScore: Int = {
    tokens.map(_.placenessScore).sum
  }

  def wordCount: Int = {
    tokens.size
  }

  def stripped: String = {
    tokens.map(_.stripped).mkString(" ")
  }

  def original: String = {
    tokens.map(_.original).mkString(" ")
  }

  def stemmed: String = {
    tokens.map(_.stem).mkString(" ")
  }

  def ofLocType(locTypes: LocationType*): Boolean = {
    locType.exists(locTypes.contains(_))
  }
}

class MultiWordLocationNameExtractor {

  def mostSuitableMultiWordNames(tokens: List[Token],
                                 foundLocationNames: Seq[MatchedInflectedLocation]
                                ): List[MultiWordName] = {

    val bestScoredMultiWordNames = tokens.zipWithIndex
      .filter(_._1.placenessScore >= 5)
      .sortBy(_._1.placenessScore)
      .reverse.slice(0, 5).map {
      case (firstToken, startIndex) =>
        MultiWordName(List(firstToken), startIndex)
    }.map {
      firstWord =>
        val tokensToUse = tokens.slice(firstWord.startIndex, firstWord.startIndex + 4)

        if (tokensToUse.size == 1) {
          firstWord
        } else {
          tokensToUse.sliding(2).map(listToTuple).takeWhile {
            case (previousToken, currentToken) =>
              currentToken.placenessScore >= 5 &&
                !(previousToken.original.endsWith(",") ||
                  previousToken.isEndOfSentence)
          }.map(_._2).foldLeft(firstWord)(_.pushWord(_))
        }
    }.sortBy(_.sumScore).reverse

    bestScoredMultiWordNames.map(attemptToMatchTypeAndNominativizeLocationName(foundLocationNames))
  }

  private def attemptToMatchTypeAndNominativizeLocationName(
                                                             foundLocationNames: Seq[MatchedInflectedLocation]
                                                           ): (MultiWordName) => MultiWordName = {
    (multiWordName: MultiWordName) => {
      val inflectedLocNamesInMWN = foundLocationNames.filter(inflectedLoc =>
        inflectedLoc.initialPos >= multiWordName.startIndex &&
          inflectedLoc.initialPos + inflectedLoc.wordCount <= multiWordName.startIndex + multiWordName.wordCount)
        .filter(_.wordCount / multiWordName.wordCount > 0.5) // the location is most of the MWN

      val locationTypes = inflectedLocNamesInMWN.map(_.inflectedLocation.location.kind).toSet
      if (locationTypes.size == 1) {
        multiWordName.copy(locType = Some(locationTypes.head))
      } else {
        multiWordName.copy(locType = Some(LocationType.UNKNOWN))
      }
    }
  }

  private def listToTuple(tokens: List[Token]): (Token, Token) = {
    assert(tokens.size == 2)
    (tokens.head, tokens.last)
  }
}
