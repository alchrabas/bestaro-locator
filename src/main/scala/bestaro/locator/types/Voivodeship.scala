package bestaro.locator.types

import bestaro.locator.util.PolishCharactersAsciizer
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

import scala.collection.immutable

sealed abstract class Voivodeship(override val entryName: String) extends EnumEntry {
  def searchString: String = {
    "województwo " + entryName.toLowerCase()
  }
}

object Voivodeship extends Enum[Voivodeship] with PlayJsonEnum[Voivodeship] {

  val values: immutable.IndexedSeq[Voivodeship] = findValues

  case object MALOPOLSKIE extends Voivodeship("MAŁOPOLSKIE")

  case object LUBUSKIE extends Voivodeship("LUBUSKIE")

  case object KUJAWSKO_POMORSKIE extends Voivodeship("KUJAWSKO-POMORSKIE")

  case object POMORSKIE extends Voivodeship("POMORSKIE")

  case object SWIETOKRZYSKIE extends Voivodeship("ŚWIĘTOKRZYSKIE")

  case object SLASKIE extends Voivodeship("ŚLĄSKIE")

  case object OPOLSKIE extends Voivodeship("OPOLSKIE")

  case object LODZKIE extends Voivodeship("ŁÓDZKIE")

  case object ZACHODNIOPOMORSKIE extends Voivodeship("ZACHODNIOPOMORSKIE")

  case object LUBELSKIE extends Voivodeship("LUBELSKIE")

  case object MAZOWIECKIE extends Voivodeship("MAZOWIECKIE")

  case object PODLASKIE extends Voivodeship("PODLASKIE")

  case object DOLNOSLASKIE extends Voivodeship("DOLNOŚLĄSKIE")

  case object PODKARPACKIE extends Voivodeship("PODKARPACKIE")

  case object WIELKOPOLSKIE extends Voivodeship("WIELKOPOLSKIE")

  case object WARMINSKO_MAZURSKIE extends Voivodeship("WARMIŃSKO-MAZURSKIE")

}

object VoivodeshipNameVariants {

  private val asciizer = new PolishCharactersAsciizer

  private def appendVoivodeshipPrefixAndAsciizedForm(name: String): Seq[String] = {
    Seq(
      name,
      asciizer.convertToAscii(name),
      "województwo " + name,
      "wojewodztwo " + asciizer.convertToAscii(name)
    )
  }

  /**
    * All lowercase variants of names of Polish voivodeships
    */
  val VARIANTS: Map[Voivodeship with Product with Serializable, Seq[String]] = Map(
    Voivodeship.MALOPOLSKIE -> Seq("małopolskie", "małopolska"),
    Voivodeship.LUBUSKIE -> Seq("lubuskie"),
    Voivodeship.KUJAWSKO_POMORSKIE -> Seq("kujawsko-pomorskie"),
    Voivodeship.POMORSKIE -> Seq("pomorskie", "pomorze"),
    Voivodeship.SWIETOKRZYSKIE -> Seq("świętokrzyskie"),
    Voivodeship.SLASKIE -> Seq("śląskie", "śląsk"),
    Voivodeship.OPOLSKIE -> Seq("opolskie"),
    Voivodeship.LODZKIE -> Seq("łódzkie"),
    Voivodeship.ZACHODNIOPOMORSKIE -> Seq("zachodniopomorskie"),
    Voivodeship.LUBELSKIE -> Seq("lubelskie"),
    Voivodeship.MAZOWIECKIE -> Seq("mazowieckie", "mazowsze"),
    Voivodeship.PODLASKIE -> Seq("podlaskie", "podlasie"),
    Voivodeship.DOLNOSLASKIE -> Seq("dolnośląskie", "dolny śląsk"),
    Voivodeship.PODKARPACKIE -> Seq("podkarpackie", "podkarpacie"),
    Voivodeship.WIELKOPOLSKIE -> Seq("wielkopolskie", "wielkopolska"),
    Voivodeship.WARMINSKO_MAZURSKIE -> Seq("warmińsko-mazurskie")
  ).mapValues(_.flatMap(appendVoivodeshipPrefixAndAsciizedForm))
}
