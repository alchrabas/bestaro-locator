package bestaro.locator.inflection

import java.io.InputStream
import java.util.zip.ZipInputStream
import scala.collection.mutable
import scala.io.Source

class PolishAspellInflectionAnalyzer extends InflectionAnalyzer {

  def learnAboutCases(): Map[String, Map[String, Set[String]]] = {
    val aspellDictionaryInputStream = readAspellDictionaryFromArchive
    val aspellDictReader = Source.fromInputStream(aspellDictionaryInputStream, "UTF-8")
    val genetivusReplacements = new mutable.HashMap[String, Set[String]]
    val locativusReplacements = new mutable.HashMap[String, Set[String]]
    aspellDictReader.getLines.foreach { line =>
      val wordVariants = line.split("\\s+").toSeq
      val strippedWordVariants = wordVariants.map(baseNameProducer.strippedForStemming)
      val results = getWordData(strippedWordVariants)

      getDifferenceInSuffixesOfInflectedWord(results, "gen", isSingular)
        .filter(suffixNotTooLong).foreach(addToMap(genetivusReplacements))
      getDifferenceInSuffixesOfInflectedWord(results, "gen", isPlural)
        .filter(suffixNotTooLong).foreach(addToMap(genetivusReplacements))
      getDifferenceInSuffixesOfInflectedWord(results, "loc", isSingular)
        .filter(suffixNotTooLong).foreach(addToMap(locativusReplacements))
      getDifferenceInSuffixesOfInflectedWord(results, "loc", isPlural)
        .filter(suffixNotTooLong).foreach(addToMap(locativusReplacements))
    }

    Map(
      "genetivus" -> mergeMaps(genetivusReplacements.toMap, supplementaryGenetivusSuffixes),
      "locativus" -> mergeMaps(locativusReplacements.toMap, supplementaryLocativusSuffixes)
    )
  }

  private def readAspellDictionaryFromArchive: InputStream = {
    val zippedAspellDictionary = new ZipInputStream(getClass.getClassLoader.getResourceAsStream("aspell_dictionary.zip"))
    val entry = zippedAspellDictionary.getNextEntry
    if (entry.getName != "aspell_dictionary") {
      throw new IllegalStateException("Invalid aspell_dictionary.zip - it doesn't have a single file aspell_dictionary")
    }
    zippedAspellDictionary
  }
}
