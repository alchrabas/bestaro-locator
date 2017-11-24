package bestaro.locator

import java.lang.reflect.Type

import bestaro.locator.inflection.InflectedLocation
import bestaro.locator.types.{Location, Voivodeship}
import com.google.gson._
import com.google.gson.reflect.TypeToken
import com.google.maps.model.GeocodingResult
import org.sqlite.SQLiteConfig
import play.api.libs.json.Json
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.SQLiteProfile.api._
import slick.jdbc.SQLiteProfile.backend.DatabaseDef
import slick.jdbc.meta.MTable

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


class LocatorDatabase(databaseFilePath: String) {

  lazy val db: DatabaseDef = {
    val sqliteConfig = new SQLiteConfig
    sqliteConfig.setJournalMode(SQLiteConfig.JournalMode.WAL)
    sqliteConfig.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL)

    val dbURL = "jdbc:sqlite:" + databaseFilePath

    val dbHandle = Database.forURL(dbURL, driver = "org.sqlite.JDBC",
      executor = AsyncExecutor("test2", minThreads = 1, queueSize = 1000,
        maxThreads = 1, maxConnections = 1),
      prop = sqliteConfig.toProperties
    )
    createSchemaIfNotExists(dbHandle)
    dbHandle
  }

  private def createSchemaIfNotExists(db: DatabaseDef): Unit = {
    val toCreate = List(googleCacheEntries, inflectedLocations)

    val existingTables = Await.result(db.run(MTable.getTables), Duration.Inf)

    val names = existingTables.map(mTable => mTable.name.name)
    val createIfNotExist = toCreate.filter(table =>
      !names.contains(table.baseTableRow.tableName)
    ).map(_.schema.create)
    Await.result(db.run(DBIO.sequence(createIfNotExist)), Duration.Inf)
  }

  private type googleResults = java.util.List[GeocodingResult]

  case class GoogleCacheEntry(queryString: String,
                              results: googleResults,
                              createTimestamp: Long
                             )

  private val gson = new Gson()
  private val gsonType = new TypeToken[googleResults]() {}.getType

  private implicit val listOfResults: JdbcType[googleResults]
    with BaseTypedType[googleResults] = MappedColumnType.base[googleResults, String](
    results => gson.toJson(results),
    gson.fromJson[googleResults](_, gsonType)
  )

  def retrieveFromCache(queryString: String): Option[googleResults] = {
    Await.result(
      db.run(
        googleCacheEntries.filter(_.queryString === queryString)
          .map(_.results).result
      ), Duration.Inf).headOption
  }

  def saveInCache(googleCacheEntry: GoogleCacheEntry): Unit = {
    db.run(
      googleCacheEntries += googleCacheEntry
    )
  }

  def allInflectedLocations(voivodeshipRestriction: Option[Voivodeship], firstWord: String): Future[Seq[InflectedLocation]] = {
    db.run(
      inflectedLocations
        .filter[Rep[Boolean]](
        if (voivodeshipRestriction.isEmpty) {
          _: InflectedLocations => false
        } else {
          a => a.voivodeship === voivodeshipRestriction.get
        })
        .filter(_.firstWord === firstWord).result
    )
  }

  def allInflectedLocations(): Future[Seq[InflectedLocation]] = {
    db.run(inflectedLocations.result)
  }

  def storeInflectedLocations(locationsToSave: Seq[InflectedLocation]): Future[Option[Int]] = {
    db.run(
      (inflectedLocations ++= locationsToSave)
        .transactionally
    )
  }

  def inflectedLocationsExist(): Future[Boolean] = {
    db.run(inflectedLocations.exists.result)
  }

  class GoogleCacheEntries(tag: Tag) extends Table[GoogleCacheEntry](tag, "google_cache_entries") {

    def queryString = column[String]("query_string", O.PrimaryKey)

    def results = column[googleResults]("collected_timestamp")

    def createTimestamp = column[Long]("create_timestamp")

    def * = (queryString, results, createTimestamp) <> (GoogleCacheEntry.tupled, GoogleCacheEntry.unapply)
  }

  val googleCacheEntries = TableQuery[GoogleCacheEntries]

  class OptionSerializer extends JsonSerializer[Option[Any]] {
    def serialize(src: Option[Any], typeOfSrc: Type, context: JsonSerializationContext): JsonElement = {
      src match {
        case None => JsonNull.INSTANCE
        case Some(v) => context.serialize(v)
      }
    }
  }

  private implicit val locationColumn: JdbcType[Location]
    with BaseTypedType[Location] = MappedColumnType.base[Location, String](
    results => Json.stringify(Json.toJson(results)),
    Json.parse(_).as[Location]
  )

  private implicit val voivodeshipColumn: JdbcType[Voivodeship]
    with BaseTypedType[Voivodeship] = MappedColumnType.base[Voivodeship, String](
    results => Json.stringify(Json.toJson(results)),
    Json.parse(_).as[Voivodeship]
  )

  class InflectedLocations(tag: Tag) extends Table[InflectedLocation](tag, "inflected_locations") {

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def stripped = column[String]("stripped")

    def firstWord = column[String]("first_word")

    def location = column[Location]("location")

    def voivodeship = column[Voivodeship]("voivodeship")

    def * = (stripped, firstWord, location, voivodeship) <> (toModel, fromModel)

    def strippedIndex = index("stripped_idx", Tuple1(id))

    private def toModel(a: (String, String, Location, Voivodeship)): InflectedLocation = {
      InflectedLocation(a._1, a._3, a._4)
    }

    private def fromModel(a: InflectedLocation): Option[(String, String, Location, Voivodeship)] = {
      Some((a.stripped, a.stripped.split(" ")(0), a.location, a.voivodeship))
    }
  }

  val inflectedLocations = TableQuery[InflectedLocations]
}
