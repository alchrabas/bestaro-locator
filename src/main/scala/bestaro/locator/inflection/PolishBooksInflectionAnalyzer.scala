package bestaro.locator.inflection

import java.io.File

import scala.io.Source

class PolishBooksInflectionAnalyzer extends InflectionAnalyzer {

  def learnAboutCases(): Map[String, Map[String, Set[String]]] = {
    Map(
      "genetivus" -> learnAboutCasesFromBooks("gen"),
      "locativus" -> learnAboutCasesFromBooks("loc")
    )
  }

  private def learnAboutCasesFromBooks(caseName: String): Map[String, Set[String]] = {
    val polishTextsDirectory = new File(getClass.getClassLoader.getResource("polish_texts").getFile)
    if (!polishTextsDirectory.isDirectory) {
      throw new IllegalStateException("polish_texts/ is not a directory!")
    }

    polishTextsDirectory.listFiles()
      .map(learnAboutCaseFromBook(caseName))
      .reduce(mergeMaps) ++ supplementarySuffixes(caseName)
  }


  private def learnAboutCaseFromBook(caseName: String): File => Map[String, Set[String]] = {
    bookFile => {
      val bufferedSource = Source.fromFile(bookFile)
      val allMappings = bufferedSource.getLines.map { line =>
        val strippedWords = line.split("\\s+").toList.map(baseNameProducer.strippedForStemming)

        val allLookupResults = getWordData(strippedWords)
        val allTransformationsToGenetivus = allLookupResults
          .filter(isSingular)
          .filter(getCases(_).contains(caseName))
          .filter(_.getStem.length() >= 3)
          .map(lookupResult => {
            val inflectedWord = lookupResult.getWord.toString
            val wordInNominativus = lookupResult.getStem.toString
            val commonPrefix = longestCommonPrefix(inflectedWord, wordInNominativus)

            (remainderOfString(wordInNominativus, commonPrefix),
              remainderOfString(inflectedWord, commonPrefix))
          })
          .filter(suffixNotTooLong)

        allTransformationsToGenetivus
      }.flatten.toList.groupBy(_._1).map { case (k, v) => (k, v.map(_._2).toSet) }
      bufferedSource.close

      allMappings
    }
  }

}
