package bestaro.locator.extractors

import java.util

import bestaro.locator.LocatorDatabase
import bestaro.locator.inflection.{InflectedLocation, PolishInflectedTownNamesGenerator}
import bestaro.locator.types.Voivodeship

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

case class MatchedInflectedLocation(inflectedLocation: InflectedLocation, initialPos: Int, wordCount: Int)

class InflectedTownNamesExtractor(database: LocatorDatabase, memoryCache: Boolean) {

  private var townEntryByVoivodeshipAndFirstWord = new util.HashMap[Voivodeship, util.HashMap[String, Seq[InflectedLocation]]]()

  def findLocationNamesFromDatabase(tokens: List[String],
                                    voivodeshipRestriction: Option[Voivodeship]
                                   ): Seq[MatchedInflectedLocation] = {

    if (!Await.result(database.inflectedLocationsExist(), Duration.Inf)) {
      val inflectedTownNamesGenerator = new PolishInflectedTownNamesGenerator()
      val allInflectedLocations = inflectedTownNamesGenerator.generateBinaryInflectedTownNamesCache()

      Await.result(database.storeInflectedLocations(allInflectedLocations), Duration.Inf)
    }

    val potentialMatches = tokens
      .zipWithIndex
      .flatMap { case (token, position) =>
        val firstWord = token.split(" ")(0)
        townsForSpecifiedVoivodeshipAndFirstWord(voivodeshipRestriction, firstWord)
          .map(inflectedLocaton => MatchedInflectedLocation(inflectedLocaton, position,
            inflectedLocaton.stripped.split(" ").length))
      }
    potentialMatches.filter {
      townEntryMatch =>
        val townNameTokens = townEntryMatch.inflectedLocation.stripped.split(" ")
        val tokensToUse = tokens.slice(
          townEntryMatch.initialPos,
          townEntryMatch.initialPos + townNameTokens.length
        )
        (townEntryMatch.initialPos + townNameTokens.length <= tokens.length) &&
          tokensToUse.zip(townNameTokens).forall {
            case (token, townNamePart) => token == townNamePart
          }
    }
  }

  private def townsForSpecifiedVoivodeshipAndFirstWord(
                                                        voivodeshipRestriction: Option[Voivodeship],
                                                        firstWord: String
                                                      ): Seq[InflectedLocation] = {
    if (memoryCache) {
      if (townEntryByVoivodeshipAndFirstWord.isEmpty) {
        val allInflectedLocations = Await.result(database.allInflectedLocations(), Duration.Inf)
        townEntryByVoivodeshipAndFirstWord = createCache(allInflectedLocations)
      }
      if (voivodeshipRestriction.isEmpty) { // all voivodeships will do
        townEntryByVoivodeshipAndFirstWord.values().asScala
          .flatMap(a => a.getOrDefault(firstWord, Seq())).toSeq
      } else {
        townEntryByVoivodeshipAndFirstWord.get(voivodeshipRestriction.get).getOrDefault(firstWord, Seq())
      }
    } else {
      Await.result(
        database.allInflectedLocations(voivodeshipRestriction, firstWord),
        Duration.Inf
      )
    }
  }

  private def createCache(allInflectedLocations: Seq[InflectedLocation]): util.HashMap[Voivodeship, util.HashMap[String, Seq[InflectedLocation]]] = {
    val inflectedFormsByVoivodeship = allInflectedLocations.groupBy(_.voivodeship)
    val scalaMap = inflectedFormsByVoivodeship
      .mapValues(_.groupBy(townEntry => townEntry.stripped.split(" ")(0)))
      .mapValues { scalaMap =>
        val newMap = new util.HashMap[String, Seq[InflectedLocation]]()
        scalaMap.foreach { case (a, b) => newMap.put(a, b) }
        newMap
      }
    val newMap = new util.HashMap[Voivodeship, util.HashMap[String, Seq[InflectedLocation]]]()
    scalaMap.foreach { case (a, b) => newMap.put(a, b) }
    newMap
  }
}
