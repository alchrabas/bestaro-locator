# Extractor of street/town names from Polish plaintext

Bestaro-Locator is a library originally created as a part of [Bestaro](https://github.com/alchrabas/bestaro) - a library that collects, processes and displays data about lost and found pets in Poland.

It's accepts a String and then tries to find the subsequences which are most likely to represent places. It works with the assumption that there is some location mentioned in the text and the text is not very long - so it only finds a single, most probable one. In other words, it's adjusted to the form of short announcements. I didn't test it for other input data.


After the initial phase of processing, the most probable location name then queried using the Google Geocoding Service. If Geocoding API says the location is valid, then it's accepted and returned together with lat/lon coordinates so the place can be put onto the map. If the location is invalid, then the API is queried using the second-best candidate and so on.

That's why it's necessary to specify google geocoding API key, otherwise the library wouldn't be able to evaluate the result.
Please note that Google Terms & Conditions claim that you can perform at most 2500 queries a day (for free) and the data you obtain can be stored/cached somewhere, but eventually it must be presented on the map provided by Google Maps.

The practical efficiency of finding the most correct location on my reference dataset was around 80%, which I find pretty nice.

Also, when you run the library, you'll probably notice that the first run of `GoogleLocationExtractor.extractLocation` is extremely slow. That's because of initialization of in-memory cache of inflected town names.

### What is used?
My idea was to evaluate every word in the input text for probability of being a location. It's based on a few simple metrics:
 - Words after specific prepositions like "na" or "w" ("on", "in")
 - Capitalized words which are not on the beginning of the sentence
 - Words after specific words like "ul.", "pl." ("street", "square")
 - Words existing in the builtin database of inflected city names

It's also possible to specify a predicate for ignoring specific words. For example in Bestaro I've specified a list of streets where animal shelters are located, because they caused lots of false-positives.

Then, when each word is evaluated, the library tries to merge multiple words into a list of potential subsequences and queries Geocoding API with them.

# How to use it?

Very easy. First you need to specify path where SQLite database (for Google Geocoding and city name cache) should be created. To do so, you need to create `LocatorDatabase` and then instantiate `GoogleLocationExtractor`.

The `GoogleLocationExtractor.extractLocation` method returns a list of tokens with their `placenessScore` (evaluation of single tokens indicating which one is the most likely to be part of location name) and a list of FullLocations. Currently it's a list of 0 or 1, but it may change in the future. The returned FullLocation has the missing Option fields set.

Example in Scala, but it should work well in anything JVM-ish:

```
val db = new LocatorDatabase("locator-db.sqlite")
val locationExtractor = new GoogleLocationExtractor(db, "YOUR GOOGLE API KEY")

// you can set Some instead of None if you already know something
// about the location, for example the voivodeship.
val alreadyKnownLocation = FullLocation(None, None, Voivodeship.MALOPOLSKIE, None)

val tokenizer = new Tokenizer()
val tokens = tokenizer.tokenize("Zapraszam na ważne wydarzenie, które " + 
	"odbędzie się w gmachu na ul. Mickiewicza")
val (stemmedTokens, matchedFullLocations) = locationExtractor.extractLocation(tokens, alreadyKnownLocation)

println(matchedFullLocations)
```

It'll return something like that (after adding a few newlines):
```
>>> List(MatchedFullLocation(
	FullLocation(
		Some(Location(mickiewicza,Mickiewicza,LocationType(street),None)),
		Some(Location(tarnów,Tarnów,LocationType(street),None)),
		Some(MALOPOLSKIE),
		Some(Coordinate(50.0152937,20.9905352))),
	10,2))
```

So it's Mickiewicza Street in Tarnów. This street definetely exists but it's not possible to guess if it's the correct town, because there's no such information in the message. So let's try with a bit different message, which also mentions the city, even though it is inflected and mentioned in not very clear way:

```
val tokens = tokenizer.tokenize("Zapraszam na ważne wydarzenie, które " +
	"odbędzie się w gmachu na ul. Mickiewicza. To sam środek Krakowa. Obecność obowiązkowa!")
val (stemmedTokens, matchedFullLocations) = locationExtractor.extractLocation(tokens, alreadyKnownLocation)

println(matchedFullLocations)
```

It yields:
```
>>> List(MatchedFullLocation(
	FullLocation(
		Some(Location(aleja adama mickiewicza,aleja Adama Mickiewicza,LocationType(street),None)),
		Some(Location(kraków,Kraków,LocationType(street),None)),
		Some(MALOPOLSKIE),
		Some(Coordinate(50.0641881,19.9241808))),
	10,2))
```

So as you can see if the city name is mentioned in the message, then it's used to prioritize potential matches of streets in this town.


# FAQ
**Q: Can I use this library for other language, for example English?**  
A: Right now it's meant only for Polish with little possibility to configure it. For example list of Voivodeships is hard-coded for Poland. Maybe it will become more configurable in the future, but doing it for Polish meant solving lots of problems which just don't exist in languages where the name of the town/street is not inflected.  
IMO the best bet is to look for some other library or create a new one, which is focused on the list of all town and street names. For example for English you could probably use Nominatim instead of Google Maps (because it has much less restrictions). Google Maps is necessary for Polish, because, unlike Nominatin, it handles inflection pretty well.

## How to release a new version

Create and push a new tag.
