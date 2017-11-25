package bestaro.locator.extractors

import java.io.FileInputStream
import java.util.{Date, Properties}

import bestaro.locator.LocatorDatabase
import com.google.maps.model.GeocodingResult
import com.google.maps.{GeoApiContext, GeocodingApi}

import scala.collection.JavaConverters._

case class CacheEfficiency(cacheHits: Long, allQueries: Long) {
  def incrementCacheHits(): CacheEfficiency = copy(cacheHits = cacheHits + 1, allQueries = allQueries + 1)

  def incrementNewQueries(): CacheEfficiency = copy(allQueries = allQueries + 1)

  def cacheEfficiency: Double = cacheHits / allQueries
}

class CachedGoogleApiClient(locatorDatabase: LocatorDatabase,
                            googleApiKey: String,
                            requestLogger: String => Unit = _ => Unit
                           ) {

  private var _cacheEfficiencyMetrics = CacheEfficiency(0, 0)

  private type listOfResults = java.util.List[GeocodingResult]


  def search(queryString: String): List[GeocodingResult] = {
    locatorDatabase
      .retrieveFromCache(queryString)
      .map { a => recordCacheHit(); a }
      .getOrElse {
        val context = new GeoApiContext.Builder()
          .apiKey(googleApiKey)
          .queryRateLimit(40)
          .build
        val results = GeocodingApi.geocode(context, queryString)
          .language("pl").await.toList.asJava
        locatorDatabase.saveInCache(locatorDatabase.GoogleCacheEntry(queryString, results, new Date().getTime))

        recordNewQuery()
        requestLogger(queryString)

        results
      }.asScala.toList
  }

  private def recordCacheHit(): Unit =
    _cacheEfficiencyMetrics = _cacheEfficiencyMetrics.incrementCacheHits()

  private def recordNewQuery(): Unit =
    _cacheEfficiencyMetrics = _cacheEfficiencyMetrics.incrementNewQueries()

  def cacheEfficiencyMetrics: CacheEfficiency = _cacheEfficiencyMetrics
}
