package bestaro.locator.types

import play.api.libs.json.{Json, OFormat}

case class FullLocation(primary: Option[Location], secondary: Option[Location],
                        voivodeship: Option[Voivodeship], coordinate: Option[Coordinate])

object FullLocation {
  implicit val fullLocationFormat: OFormat[FullLocation] = Json.format[FullLocation]
}


case class Coordinate(lat: Double, lon: Double)

object Coordinate {
  implicit val coordinateFormat: OFormat[Coordinate] = Json.format[Coordinate]
}
