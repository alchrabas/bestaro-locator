package bestaro.locator.extractors

import bestaro.locator.LocatorDatabase
import bestaro.locator.types.{Token, _}
import com.google.maps.model.{AddressComponent, AddressComponentType, GeocodingResult}

import scala.collection.mutable.ListBuffer

class GoogleLocationExtractor(locatorDatabase: LocatorDatabase, googleApiKey: String, memoryCache: Boolean = true)
  extends AbstractLocationExtractor(locatorDatabase, memoryCache) {

  private val geocodingClient = new CachedGoogleApiClient(locatorDatabase, googleApiKey, countingLogger())

  private def countingLogger() = {
    var requests = 0
    _: String => {
      requests += 1
      if (requests % 100 == 0) {
        println(s"Done $requests requests to Google API")
      }
    }
  }

  def cacheEfficiencyMetrics: CacheEfficiency = geocodingClient.cacheEfficiencyMetrics

  override protected def specificExtract(alreadyKnownLocation: FullLocation,
                                         stemmedTokens: List[Token],
                                         foundLocationNames: Seq[MatchedInflectedLocation]):
  (ListBuffer[Token], ListBuffer[MatchedFullLocation]) = {
    val mutableTokens = stemmedTokens.to[ListBuffer]

    val multiWordLocationNameExtractor = new MultiWordLocationNameExtractor
    val multiWordNames = multiWordLocationNameExtractor.mostSuitableMultiWordNames(stemmedTokens, foundLocationNames)
    println(multiWordNames.map(_.stripped).mkString(";; "))

    val bestResults = getBestResultForProposedNames(multiWordNames, alreadyKnownLocation)
      .map { case (name, results) =>
        MatchedFullLocation(fullLocationFromGeocodingResults(results, foundLocationNames, multiWordNames),
          name.startIndex, name.wordCount)
      }.to[ListBuffer]

    (mutableTokens, bestResults)
  }

  private def getBestResultForProposedNames(multiWordNames: Seq[MultiWordName],
                                            alreadyKnownLocation: FullLocation
                                           ): Option[(MultiWordName, Seq[GeocodingResult])] = {

    val (foundCities, remainderOfLocations) = multiWordNames.partition(_.ofLocType(LocationType.CITY))

    if (alreadyKnownLocation.secondary.isDefined) {
      for (street <- remainderOfLocations) {
        val result = getGeocodingResultForLocationName(street,
          alreadyKnownLocation.secondary.map(_.stripped),
          alreadyKnownLocation.voivodeship)
        if (result._2.nonEmpty) {
          return Some(result)
        }
      }
    }

    for (street <- remainderOfLocations) { // TODO create two classes for MWN to avoid Option fields
      for (city <- foundCities) {
        val result = getGeocodingResultForLocationName(street,
          Some(city.stripped),
          alreadyKnownLocation.voivodeship)
        if (result._2.nonEmpty) {
          return Some(result)
        }
      }
    }

    for (name <- multiWordNames) {
      val result = getGeocodingResultForLocationName(name,
        None,
        alreadyKnownLocation.voivodeship)
      if (result._2.nonEmpty) {
        return Some(result)
      }
    }
    None
  }

  private def getGeocodingResultForLocationName(multiWordName: MultiWordName,
                                                broadLocation: Option[String] = None,
                                                voivodeship: Option[Voivodeship] = None) = {
    val nameToSearchFor = replaceAbbreviatedNouns(multiWordName).stripped +
      broadLocation.map(", " + _).getOrElse("") +
      voivodeship.map(", " + _.searchString).getOrElse("")
    (multiWordName, geocodingClient.search(nameToSearchFor))
  }

  private def replaceAbbreviatedNouns(multiWordName: MultiWordName): MultiWordName = {
    multiWordName.copy(tokens = multiWordName.tokens
      .map(expandAndNominativizeAbbreviatedNounsPrecedingLocation)
      .filterNot(isStopword))
  }

  private def expandAndNominativizeAbbreviatedNounsPrecedingLocation(token: Token): Token = {
    val strippedReplacements = REPLACEMENTS.get(token.stripped)
    if (strippedReplacements.isDefined) {
      token.copy(stripped = strippedReplacements.get, stem = strippedReplacements.get)
    } else {
      token
    }
  }

  private def isStopword(token: Token): Boolean = {
    STOPWORDS.contains(token.stripped)
  }

  private val REPLACEMENTS = Map(
    "os" -> "osiedle",
    "al" -> "aleja",
    "ul" -> "ulica",
    "pl" -> "plac",
    "gm" -> "gmina",
    "ulicy" -> "ulica",
    "osiedlu" -> "osiedle",
    "osiedla" -> "osiedle",
    "alei" -> "aleja",
    "placu" -> "plac",
    "placem" -> "plac",
    "gminy" -> "gmina",
    "gminie" -> "gmina"
  )

  private val STOPWORDS = Set(
    "okolicy",
    "okolica",
    "pobliżu",
    "miejscowość",
    "miejscowości"
  )

  private def fullLocationFromGeocodingResults(results: Seq[GeocodingResult],
                                               foundLocationNames: Seq[MatchedInflectedLocation],
                                               multiWordNames: Seq[MultiWordName]
                                              ): FullLocation = {
    val firstResult = getBestResult(results, foundLocationNames, multiWordNames, List("kraków"))
    val voivodeshipName = firstResult.addressComponents
      .find(_.types.contains(AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_1))
      .map(_.shortName).getOrElse("UKNOWN").toLowerCase.replaceAll("województwo ", "")
    val matchedVoivodeship = Voivodeship.values.find(_.entryName.equalsIgnoreCase(voivodeshipName))

    val locationType = firstFoundType(firstResult.addressComponents)
    val primaryLocation = getMostSpecificLocation(firstResult, PRIMARY_LOCATION_TYPES)
      .map(primaryPart => Location(
        baseNameProducer.strippedForStemming(primaryPart),
        primaryPart,
        locationType))
    val secondaryLocation = getMostSpecificLocation(firstResult, SECONDARY_LOCATION_TYPES)
      .map(secondaryPart => Location(
        baseNameProducer.strippedForStemming(secondaryPart),
        secondaryPart,
        locationType))

    val coords = firstResult.geometry.location
    FullLocation(
      primaryLocation, secondaryLocation,
      matchedVoivodeship,
      Some(Coordinate(coords.lat, coords.lng))
    )
  }

  private val PRIMARY_LOCATION_TYPES = List(
    (AddressComponentType.ROUTE, LocationType.STREET),
    (AddressComponentType.STREET_ADDRESS, LocationType.STREET),
    (AddressComponentType.NEIGHBORHOOD, LocationType.ESTATE),
    (AddressComponentType.SUBLOCALITY, LocationType.DISTRICT)
  )
  private val SECONDARY_LOCATION_TYPES = List(
    (AddressComponentType.LOCALITY, LocationType.CITY),
    (AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_3, LocationType.MUNCIPALITY)
  )
  private val ALL_TYPES = PRIMARY_LOCATION_TYPES ++ SECONDARY_LOCATION_TYPES

  private def firstFoundType(addressComponents: Array[AddressComponent]): LocationType = {
    ALL_TYPES.find(requiredType => addressComponents.flatMap(_.types).contains(requiredType._1)).map(_._2)
      .getOrElse(LocationType.UNKNOWN)
  }

  private def getMostSpecificLocation(geoResult: GeocodingResult,
                                      types: Seq[(AddressComponentType, LocationType)]
                                     ): Option[String] = {
    types.map(_._1).flatMap(addressType =>
      geoResult.addressComponents.filter(isOfType(_, addressType)).map(_.longName)).headOption
  }

  private def isOfType(component: AddressComponent, addressComponentType: AddressComponentType) = {
    component.types.contains(addressComponentType)
  }

  private def getBestResult(results: Seq[GeocodingResult],
                            foundLocationNames: Seq[MatchedInflectedLocation],
                            multiWordNames: Seq[MultiWordName],
                            implicitMainTownNames: Seq[String]): GeocodingResult = {
    val resultsAndTheirTownNames = results.map(result => (result, result.addressComponents.filter(addressPart =>
      Set(AddressComponentType.LOCALITY, AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_3)
        .exists(addressPart.types.contains(_))).map(addressPart => baseNameProducer.strippedForStemming(addressPart.longName))))
    for (resultAndItsTownNames <- resultsAndTheirTownNames) {
      if (resultAndItsTownNames._2.exists(locName =>
        foundLocationNames.map(_.inflectedLocation.location.stripped).contains(locName))) {
        return resultAndItsTownNames._1
      }
    }
    for (resultAndItsTownNames <- resultsAndTheirTownNames) {
      if (resultAndItsTownNames._2.exists(implicitMainTownNames.contains(_))) {
        return resultAndItsTownNames._1
      }
    }
    results.head
  }
}
