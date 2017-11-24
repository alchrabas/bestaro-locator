package bestaro.locator.types

case class PartOfSpeech(name: String)

object PartOfSpeech {
  val ADJECTIVE = PartOfSpeech("adjective")
  val NOUN = PartOfSpeech("noun")
  val PREPOSITION = PartOfSpeech("preposition") // also used for conjunctions
  val VERB = PartOfSpeech("verb")
  val PUNCTUATED_END = PartOfSpeech("abbreviation")
  val OTHER = PartOfSpeech("other")
}

case class Gender(symbol: String)

object Gender {
  val M = Gender("masculine")
  val F = Gender("feminine")
  val N = Gender("neuter")
}

case class Importance(name: String)

object Importance {
  val PRIMARY = Importance("primary")
  val SECONDARY = Importance("secondary")
}

case class Flag(name: String)

object Flag {
  val PUNCTUATED_WORD = Flag("punctuated_word")
  val EMPTY_TOKEN = Flag("empty_token")
  val NOUN_PRECEDING_LOCATION_NAME = Flag("noun_preceding_location_name")
}

case class Token(
                  original: String,
                  stripped: String,
                  stem: String,
                  partsOfSpeech: List[PartOfSpeech],
                  genders: List[Gender],
                  placenessScore: Int,
                  importance: Importance = Importance.PRIMARY,
                  flags: Set[Flag] = Set()
                ) {
  override def toString: String = {
    original + " (" + stem + ")[" + placenessScore + "]"
  }

  def withAlteredPlacenessScore(alteredBy: Int): Token = {
    copy(placenessScore = placenessScore + alteredBy)
  }

  def isEndOfSentence: Boolean = {
    original.nonEmpty &&
      ((original.endsWith(".") && !flags.contains(Flag.PUNCTUATED_WORD)) ||
        Set('!', '\n', '?').contains(original.last))
  }
}
