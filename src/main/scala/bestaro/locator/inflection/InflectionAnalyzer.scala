package bestaro.locator.inflection

import bestaro.locator.util.BaseNameProducer
import morfologik.stemming.WordData
import morfologik.stemming.polish.PolishStemmer

import scala.collection.JavaConverters._
import scala.collection.mutable

abstract class InflectionAnalyzer {

  protected val baseNameProducer = new BaseNameProducer

  protected val polishStemmer = new PolishStemmer

  def learnAboutCases(): Map[String, Map[String, Set[String]]]

  protected def getDifferenceInSuffixesOfInflectedWord(results: Seq[WordData], caseName: String,
                                                       numberPredicate: WordData => Boolean): Option[(String, String)] = {
    val wordInNominativus = results.find(result => getCases(result).contains("nom") &&
      numberPredicate(result)).map(_.getWord.toString.toLowerCase)
    val inflectedName = results.find(result => getCases(result).contains(caseName) &&
      numberPredicate(result)).map(_.getWord.toString.toLowerCase)

    if (wordInNominativus.isDefined && inflectedName.isDefined) {
      val commonPrefix = longestCommonPrefix(inflectedName.get, wordInNominativus.get)
      Some((remainderOfString(wordInNominativus.get, commonPrefix),
        remainderOfString(inflectedName.get, commonPrefix)))
    } else {
      None
    }
  }


  protected def suffixNotTooLong(suffixReplacement: (String, String)): Boolean = {
    suffixReplacement._1.length <= 5 && suffixReplacement._2.length <= 5
  }

  protected def longestCommonPrefix(first: String, second: String): String = {
    first.zip(second).takeWhile { case (a, b) => a == b }.map(_._1).mkString
  }

  protected def getWordData(strippedWords: Seq[String]): Seq[WordData] = {
    strippedWords.flatMap(
      wiseLookup(_)
        .map(_.clone()) // idk why, but it's necessary
        .toList)
  }

  protected def getCases(lookupResult: WordData): Set[String] = {
    splitIntoTokens(lookupResult).filter(CASES.contains).toSet
  }

  protected def isPlural(lookupResult: WordData): Boolean = {
    splitIntoTokens(lookupResult).contains("pl")
  }

  protected def isSingular(lookupResult: WordData): Boolean = {
    splitIntoTokens(lookupResult).contains("sg")
  }

  protected def splitIntoTokens(lookupResult: WordData): Seq[String] = {
    lookupResult.getTag.toString.split("[:.+]")
  }

  protected def addToMap(map: mutable.HashMap[String, Set[String]]): ((String, String)) => Unit = {
    case (key, value) =>
      map.put(key, map.getOrElse(key, Set()) + value)
  }

  protected def remainderOfString(subject: String, prefixToRemove: String): String = {
    subject.substring(Math.max(0, prefixToRemove.length - 2))
  }

  protected def wiseLookup(word: String): Seq[WordData] = {
    polishStemmer.lookup(word.toLowerCase).asScala.toList :::
      polishStemmer.lookup(word.toLowerCase.capitalize).asScala.toList
  }

  private val CASES = Set("nom", "gen", "acc", "dat", "inst", "loc", "voc")

  protected val supplementaryGenetivusSuffixes = Map(
    "ski" -> Set("skiego"),
    "ska" -> Set("skiej"),
    "skie" -> Set("skiego"),
    "nia" -> Set("ni"),
    "ów" -> Set("owa"),
    "ór" -> Set("oru"),
    "wa" -> Set("wy"),
    "na" -> Set("ny"),
    "ko" -> Set("ka"),
    "no" -> Set("na"),
    "ik" -> Set("ika"),
    "ice" -> Set("ic"),
    "ła" -> Set("łej"),
    "cha" -> Set("chej"),
    "rze" -> Set("rza"),
    "arz" -> Set("arza"),
    "ica" -> Set("icy"),
    "cki" -> Set("ckiego"),
    "cka" -> Set("ckiej"),
    "ckie" -> Set("ckiego"),
    "icz" -> Set("icza"),
    "ia" -> Set("iej"),
    "cz" -> Set("cza"),
    "ka" -> Set("ki"),
    "cie" -> Set("cia"),
    "ta" -> Set("ty"),
    "prz" -> Set("prza"),
    "dziec" -> Set("dźca"),
    "iec" -> Set("ca"),
    "aw" -> Set("awia"),
    "la" -> Set("li"),
    "sk" -> Set("ska"),
    "zu" -> Set("zów"),
    "wo" -> Set("wa"),
    "in" -> Set("ina"),
    "yce" -> Set("yc"),
    "lce" -> Set("lc"),
    "sto" -> Set("sta"),
    "yśl" -> Set("yśla"),
    "iąż" -> Set("iąża"),
    "mża" -> Set("mży"),
    "słą" -> Set("słą"), // "X nad Wisłą"?
    "zyń" -> Set("zynia"),
    "tyń" -> Set("tynia"),
    "om" -> Set("omia")
  )

  protected val supplementaryLocativusSuffixes = Map(
    "ski" -> Set("skim", "skiem"),
    "ska" -> Set("skiej"),
    "skie" -> Set("skim", "skiem"),
    "nia" -> Set("ni"),
    "ów" -> Set("owie"),
    "ór" -> Set("orze"),
    "wa" -> Set("wie", "wej"),
    "na" -> Set("nie"),
    "ko" -> Set("ku"),
    "no" -> Set("nie"),
    "ik" -> Set("iku"),
    "ice" -> Set("icach"),
    "ła" -> Set("łej"),
    "sła" -> Set("śle"),
    "cha" -> Set("sze"),
    "rze" -> Set("rzu"),
    "arz" -> Set("arzu"),
    "ica" -> Set("icy"),
    "cki" -> Set("ckim", "ckiem"),
    "cka" -> Set("ckiej"),
    "ckie" -> Set("ckim", "ckiem"),
    "icz" -> Set("iczu"),
    "ia" -> Set("i", "ii"),
    "cz" -> Set("czu"),
    "ka" -> Set("ce"),
    "cie" -> Set("ciu"),
    "ta" -> Set("cie"),
    "prz" -> Set("przu"),
    "dziec" -> Set("dźcu"),
    "iec" -> Set("cu"),
    "aw" -> Set("awiu"),
    "la" -> Set("li"),
    "sk" -> Set("sku"),
    "zu" -> Set("zie"),
    "wo" -> Set("wie"),
    "in" -> Set("inie"),
    "yce" -> Set("ycach"),
    "lce" -> Set("lcach"),
    "sto" -> Set("ście"),
    "yśl" -> Set("yślu"),
    "iąż" -> Set("iążu"),
    "mża" -> Set("mży"),
    "słą" -> Set("słą"), // "X nad Wisłą"?
    "zyń" -> Set("zyniu"),
    "tyń" -> Set("tyniu"),
    "lkie" -> Set("lkich"),
    "ród" -> Set("rodzie"),
    "rne" -> Set("rnym", "rnem"),
    "lne" -> Set("lnym", "lnem"),
    "ądz" -> Set("ądzu"),
    "leń" -> Set("leniu"),
    "uty" -> Set("utach"),
    "owe" -> Set("owem", "owych"),
    "om" -> Set("omiu")
  )

  protected val supplementarySuffixes = Map(
    "gen" -> supplementaryGenetivusSuffixes,
    "loc" -> supplementaryLocativusSuffixes
  )

  protected def mergeMaps(m1: Map[String, Set[String]], m2: Map[String, Set[String]]): Map[String, Set[String]] = {
    (m1.keys.toSet ++ m2.keys.toSeq.toSet).map(key =>
      key -> (m1.getOrElse(key, Set()) ++ m2.getOrElse(key, Set()))).toMap
  }
}
