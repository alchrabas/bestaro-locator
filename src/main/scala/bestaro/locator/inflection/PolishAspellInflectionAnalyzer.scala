package bestaro.locator.inflection

import java.io.File
import java.util.zip.ZipFile

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

  private def readAspellDictionaryFromArchive = {
    val zippedAspellDictionary = new File(getClass.getClassLoader.getResource("aspell_dictionary.zip").getFile)

    val zipFile = new ZipFile(zippedAspellDictionary)
    val entry = zipFile.getEntry("aspell_dictionary")

    zipFile.getInputStream(entry)
  }
}
