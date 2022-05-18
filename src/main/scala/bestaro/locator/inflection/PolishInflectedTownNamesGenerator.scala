package bestaro.locator.inflection

import bestaro.locator.types
import bestaro.locator.types.{Location, LocationType, Voivodeship}
import bestaro.locator.util.BaseNameProducer
import com.opencsv.{CSVParserBuilder, CSVReaderHeaderAwareBuilder}

import java.io.{InputStream, InputStreamReader}
import scala.collection.JavaConverters._

object PolishInflectedTownNamesGenerator {

  def main(args: Array[String]): Unit = {
    val converter = new PolishInflectedTownNamesGenerator
    converter.generateBinaryInflectedTownNamesCache()
  }
}

case class InflectedLocation(stripped: String, location: Location, voivodeship: Voivodeship)

class PolishInflectedTownNamesGenerator {

  val URZEDOWY_WYKAZ_NAZW_CSV = "gus/urzedowy_wykaz_nazw_miejscowosci_2015.csv"

  private type suffixReplacementMapType = Map[String, Set[String]]
  private type allSuffixesMapType = Map[String, suffixReplacementMapType]

  private val suffixesReadFromFile = new PolishAspellInflectionAnalyzer().learnAboutCases()

  private val genetivusSuffixes = suffixesReadFromFile("genetivus")
  private val locativusSuffixes = suffixesReadFromFile("locativus")

  private val baseNameProducer = new BaseNameProducer

  private val TOWN_NAMES_CSV = "gus/TERC_Adresowy_2017-07-17.csv"

  def loadTownEntriesFromTerytFile(): Seq[InflectedLocation] = {
    val terytEntries = csvInputStreamToMap(getClass.getClassLoader.getResourceAsStream(TOWN_NAMES_CSV))
    terytEntries
      .filterNot(row => row("NAZWA_DOD") == "województwo")
      .map(convertToTownEntry)
  }

  private def convertToTownEntry(csvEntry: Map[String, String]): InflectedLocation = {
    val originalName = csvEntry("NAZWA").toLowerCase
    val voivodeshipId = Integer.parseInt(csvEntry("WOJ"))
    val kind = csvEntry("NAZWA_DOD")

    InflectedLocation(originalName, types.Location(originalName, originalName, locationTypeByKindColumn(csvEntry)),
      VOIVODESHIP_BY_ID(voivodeshipId))
  }

  private val NAME_COLUMN = "Nazwa miejscowości "
  private val KIND_COLUMN = "Rodzaj"
  private val VOIVODESHIP_COLUMN = "Województwo"

  def loadTownEntriesFromUrzedowyWykazNazwMiejscowosciCSV(): Seq[InflectedLocation] = {
    val dataFromWykaz = csvInputStreamToMap(getClass.getClassLoader.getResourceAsStream(URZEDOWY_WYKAZ_NAZW_CSV))

    val towns = dataFromWykaz
      .filter(row =>
        Set("wieś", "miasto").contains(row(KIND_COLUMN))
      ).map {
      row =>
        val stripped = baseNameProducer.strippedForStemming(row(NAME_COLUMN))
        InflectedLocation(stripped,
          types.Location(stripped, row(NAME_COLUMN), locationTypeByKindColumn(row)),
          Voivodeship.withName(row(VOIVODESHIP_COLUMN).toUpperCase)
        )
    }

    val districts = dataFromWykaz
      .filter(_ (KIND_COLUMN).startsWith("część miasta"))
      .map {
        row =>
          val townParentName = row(KIND_COLUMN) replaceAll("część miasta ", "")
          val strippedDistrictName = baseNameProducer.strippedForStemming(row(NAME_COLUMN))
          InflectedLocation(strippedDistrictName,
            Location(strippedDistrictName,
              row(NAME_COLUMN),
              LocationType.DISTRICT,
              Some(
                Location(townParentName,
                  baseNameProducer.strippedForStemming(townParentName),
                  LocationType.CITY
                )
              )
            ),
            Voivodeship.withName(row(VOIVODESHIP_COLUMN).toUpperCase)
          )
      }

    towns ++ districts
  }

  def generateInflectedForms(townsInNominativus: Seq[InflectedLocation]): Seq[InflectedLocation] = {
    townsInNominativus.flatMap(inflectedVersionsOfName).distinct
  }

  def inflectedVersionsOfName(original: InflectedLocation): Seq[InflectedLocation] = {
    makeInflectedVersion(original, genetivusSuffixes) ++ makeInflectedVersion(original, locativusSuffixes)
  }

  private def makeInflectedVersion(nominativusTown: InflectedLocation,
                                   suffixReplacements: Map[String, Set[String]]): Seq[InflectedLocation] = {
    val wordsOfOriginalName = nominativusTown.location.stripped.split(" ")
    val allWordVariants = wordsOfOriginalName.toSeq.map {
      originalWordOfName =>
        val wordReplacements = suffixReplacements
          .filterKeys(originalWordOfName.endsWith)
          .flatMap {
            case (pattern, replacements) =>
              replacements.map(replacement =>
                replaceSuffix(originalWordOfName, pattern, replacement))
          }.toList
        originalWordOfName :: wordReplacements
    }

    generateAllWordCombinations(allWordVariants)
      .map(_.mkString(" "))
      .map(newName => nominativusTown.copy(stripped = newName))
  }

  private def generateAllWordCombinations(wordVariants: Seq[Seq[String]]): Seq[Seq[String]] = {
    if (wordVariants.isEmpty) {
      return List(List())
    }

    wordVariants.head.flatMap(variant =>
      generateAllWordCombinations(wordVariants.tail)
        .map(variant :: _.toList))
  }

  private def replaceSuffix(subject: String, pattern: String, replacement: String): String = {
    subject.substring(0, subject.length - pattern.length) + replacement
  }

  private def locationTypeByKindColumn(row: Map[String, String]): LocationType = {
    row(KIND_COLUMN) match {
      case "wieś" => LocationType.VILLAGE
      case "miasto" => LocationType.CITY
    }
  }

  def printMostIgnoredSuffixes(townsInNominativus: Seq[InflectedLocation]): Unit = {
    val mostIgnoredSuffixes = townsInNominativus
      .map(_.stripped)
      .filter(a => !genetivusSuffixes.keys.exists(a.endsWith))
      .filter(_.length >= 3)
      .map(a => a.substring(a.length - 3, a.length))
      .groupBy(a => a)
      .mapValues(_.size)

    println("# " + townsInNominativus.size + " BROKEN " + mostIgnoredSuffixes.values.sum)

    println(
      mostIgnoredSuffixes
        .toSeq.sortBy(_._2)
        .reverse.mkString("\n")
    )
  }

  def generateBinaryInflectedTownNamesCache(): Seq[InflectedLocation] = {
    generateInflectedForms(loadTownEntriesFromUrzedowyWykazNazwMiejscowosciCSV())
  }

  private def csvInputStreamToMap(csvStream: InputStream): List[Map[String, String]] = {
    val csvEntries = new CSVReaderHeaderAwareBuilder(new InputStreamReader(csvStream))
      .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
      .build()
    Iterator.continually(csvEntries.readMap())
      .takeWhile(_ != null)
      .map(_.asScala.toMap)
      .toList
  }

  val VOIVODESHIP_BY_ID = Map(
    12 -> Voivodeship.MALOPOLSKIE,
    8 -> Voivodeship.LUBUSKIE,
    4 -> Voivodeship.KUJAWSKO_POMORSKIE,
    22 -> Voivodeship.POMORSKIE,
    26 -> Voivodeship.SWIETOKRZYSKIE,
    24 -> Voivodeship.SLASKIE,
    16 -> Voivodeship.OPOLSKIE,
    10 -> Voivodeship.LODZKIE,
    32 -> Voivodeship.ZACHODNIOPOMORSKIE,
    6 -> Voivodeship.LUBELSKIE,
    14 -> Voivodeship.MAZOWIECKIE,
    20 -> Voivodeship.PODLASKIE,
    2 -> Voivodeship.DOLNOSLASKIE,
    18 -> Voivodeship.PODKARPACKIE,
    30 -> Voivodeship.WIELKOPOLSKIE,
    28 -> Voivodeship.WARMINSKO_MAZURSKIE
  )
}
