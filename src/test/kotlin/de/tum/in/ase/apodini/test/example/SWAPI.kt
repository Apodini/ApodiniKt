package de.tum.`in`.ase.apodini.test.example

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.tum.`in`.ase.apodini.*
import de.tum.`in`.ase.apodini.configuration.ConfigurationBuilder
import de.tum.`in`.ase.apodini.environment.EnvironmentKey
import de.tum.`in`.ase.apodini.environment.EnvironmentKeys
import de.tum.`in`.ase.apodini.exporter.RESTExporter
import de.tum.`in`.ase.apodini.impl.group
import de.tum.`in`.ase.apodini.impl.text
import de.tum.`in`.ase.apodini.modifiers.ModifiableComponent
import de.tum.`in`.ase.apodini.properties.PathParameter
import de.tum.`in`.ase.apodini.properties.environment
import de.tum.`in`.ase.apodini.properties.pathParameter
import de.tum.`in`.ase.apodini.types.CustomType
import de.tum.`in`.ase.apodini.types.Hidden
import de.tum.`in`.ase.apodini.types.TypeDefinition
import de.tum.`in`.ase.apodini.types.TypeDefinitionBuilder
import io.ktor.features.*
import kotlinx.coroutines.CoroutineScope

fun main() {
    SWAPI().run()
}

class SWAPI : WebService {
    override fun ComponentBuilder.invoke() {
        text("Welcome to a Star Wars API")

        entity("films", SWAPIStore::films) { filmId ->
            itemRelationship("characters", filmId) { id ->
                charactersByFilm[id] ?: throw NotFoundException()
            }
            itemRelationship("planets", filmId) { id ->
                planetsByFilm[id] ?: throw NotFoundException()
            }
            itemRelationship("species", filmId) { id ->
                speciesByFilm[id] ?: throw NotFoundException()
            }
            itemRelationship("starships", filmId) { id ->
                starshipsByFilm[id] ?: throw NotFoundException()
            }
        }

        entity("people", SWAPIStore::people) { personId ->
            itemRelationship("films", personId) { id ->
                filmsByCharacter[id] ?: throw NotFoundException()
            }
            itemRelationship("starships", personId) { id ->
                starshipsByPilot[id] ?: throw NotFoundException()
            }
        }

        entity("planets", SWAPIStore::planets) { planetId ->
            itemRelationship("films", planetId) { id ->
                filmsByPlanet[id] ?: throw NotFoundException()
            }
            itemRelationship("people", planetId) { id ->
                charactersByPlanet[id] ?: throw NotFoundException()
            }
            itemRelationship("people", planetId) { id ->
                speciesByPlanet[id] ?: throw NotFoundException()
            }
        }

        entity("species", SWAPIStore::species) { speciesId ->
            itemRelationship("films", speciesId) { id ->
                filmsBySpecies[id] ?: throw NotFoundException()
            }
            itemRelationship("people", speciesId) { id ->
                charactersBySpecies[id] ?: throw NotFoundException()
            }
        }

        entity("starships", SWAPIStore::starships) { shipId ->
            itemRelationship("films", shipId) { id ->
                filmsByStarship[id] ?: throw NotFoundException()
            }

            itemRelationship("pilots", shipId) { id ->
                pilotsByStarship[id] ?: throw NotFoundException()
            }
        }
    }

    override fun ConfigurationBuilder.configure() {
        use(RESTExporter(port = 8080))

        environment {
            store {
                SWAPIStore.fromResources()
            }
        }
    }
}

private inline fun <reified T> ComponentBuilder.entity(
    name: String,
    noinline items: SWAPIStore.() -> Map<Int, T>,
    crossinline related: ComponentBuilder.(PathParameter) -> Unit = {}
) {
    group(name) {
        entity(items, related)
    }
}

private inline fun <reified T> ComponentBuilder.entity(
    noinline items: SWAPIStore.() -> Map<Int, T>,
    crossinline related: ComponentBuilder.(PathParameter) -> Unit = {}
) {
    val id = pathParameter()

    allItems(items)

    group(id) {
        item(id, items)
        related(id)
    }
}

private inline fun <reified T> ComponentBuilder.allItems(noinline items: SWAPIStore.() -> Map<Int, T>): ModifiableComponent<*> {
    return +AllItemsHandler(items)
}

private inline fun <reified T> ComponentBuilder.item(id: PathParameter, noinline items: SWAPIStore.() -> Map<Int, T>): ModifiableComponent<*> {
    return +ItemHandler(id, items)
}

private inline fun <reified T : Any> ComponentBuilder.itemRelationship(name: String, id: PathParameter, noinline items: SWAPIStore.(Int) -> T): ModifiableComponent<*> {
    return group(name) {
        itemRelationship(id, items)
    }
}

private inline fun <reified T> ComponentBuilder.itemRelationship(id: PathParameter, noinline items: SWAPIStore.(Int) -> T): ModifiableComponent<*> {
    return +ItemRelationshipHandler(id, items)
}

private class AllItemsHandler<T>(val items: SWAPIStore.() -> Map<Int, T>) : Handler<List<T>> {
    val store by environment { store }
    override suspend fun CoroutineScope.compute(): List<T> {
        return store.items().values.toList()
    }
}

private class ItemHandler<T>(id: PathParameter, val items: SWAPIStore.() -> Map<Int, T>) : Handler<T> {
    val id by id
    val store by environment { store }

    override suspend fun CoroutineScope.compute(): T {
        return id.toIntOrNull()?.let { store.items()[it] } ?: throw NotFoundException()
    }
}

private class ItemRelationshipHandler<T>(id: PathParameter, val items: SWAPIStore.(Int) -> T) : Handler<T> {
    val id by id
    val store by environment { store }

    override suspend fun CoroutineScope.compute(): T {
        return id.toIntOrNull()?.let { store.items(it) } ?: throw NotFoundException()
    }
}

private class SWAPIStore(
    val films: Map<Int, Film>,
    val people: Map<Int, Person>,
    val planets: Map<Int, Planet>,
    val species: Map<Int, Species>,
    val starships: Map<Int, Starship>
) {
    val charactersByFilm by lazy { films.mapValues { film -> film.value.characters.mapNotNull { people[it] } } }
    val planetsByFilm by lazy { films.mapValues { film -> film.value.planets.mapNotNull { planets[it] } } }
    val speciesByFilm by lazy { films.mapValues { film -> film.value.species.mapNotNull { species[it] } } }
    val starshipsByFilm by lazy { films.mapValues { film -> film.value.starships.mapNotNull { starships[it] } } }

    val filmsByCharacter by lazy { films.mapValues { it.value.characters }.flip().mapValues { ids -> ids.value.mapNotNull { films[it] } } }
    val filmsByPlanet by lazy { films.mapValues { it.value.planets }.flip().mapValues { ids -> ids.value.mapNotNull { films[it] } } }
    val filmsBySpecies by lazy { films.mapValues { it.value.species }.flip().mapValues { ids -> ids.value.mapNotNull { films[it] } } }
    val filmsByStarship by lazy { films.mapValues { it.value.starships }.flip().mapValues { ids -> ids.value.mapNotNull { films[it] } } }

    val pilotsByStarship by lazy { starships.mapValues { ship -> ship.value.pilots.mapNotNull { people[it] } } }
    val starshipsByPilot by lazy { starships.mapValues { it.value.pilots }.flip().mapValues { ids -> ids.value.mapNotNull { films[it] } } }

    val charactersByPlanet by lazy { people.mapValues { listOf(it.value.homeworld) }.flip().mapValues { ids -> ids.value.mapNotNull { people[it] } } }
    val speciesByPlanet by lazy { species.mapValues { listOf(it.value.homeworld) }.flip().mapValues { ids -> ids.value.mapNotNull { species[it] } } }

    val charactersBySpecies by lazy { species.mapValues { species -> species.value.people.mapNotNull { people[it] } } }

    companion object : EnvironmentKey<SWAPIStore>() {
        override val default = SWAPIStore(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap())
        fun fromResources() = SWAPIStore(
            parse("films") { readValue(it) },
            parse("people") { readValue(it) },
            parse("planets") { readValue(it) },
            parse("species") { readValue(it) },
            parse("starships") { readValue(it) },
        ).apply {
            species.forEach { (id, item) ->
                item.people.forEach { personId ->
                    people[personId]?.species = id
                }
            }
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
private inline fun <reified T : Identifiable> parse(
    fileName: String,
    read: ObjectMapper.(java.net.URL) -> List<Parsed<T>>
): Map<Int, T> {
    val file = SWAPI::class.java.getResource("/swapi/$fileName.json")
    val mapper = jacksonObjectMapper()
    val parsed = mapper.read(file)
    parsed.forEach { it.value.id = it.id }
    return parsed.map { it.id to it.value }.toMap()
}

@JsonIgnoreProperties(ignoreUnknown = true)
private class Parsed<T : Identifiable>(
    @JsonProperty("pk")
    val id: Int,
    @JsonProperty("fields")
    val value: T
)

private interface Identifiable {
    var id: Int?
}

private val EnvironmentKeys.store: EnvironmentKey<SWAPIStore>
    get() = SWAPIStore

@JsonIgnoreProperties(ignoreUnknown = true)
private class Film(
    val producer: String,
    val title: String,
    @JsonProperty("episode_id")
    val episodeId: String,
    val director: String,
    @JsonProperty("opening_crawl")
    val openingCrawl: String,
    @Hidden
    val starships: List<Int>,
    @Hidden
    val planets: List<Int>,
    @Hidden
    val characters: List<Int>,
    @Hidden
    val species: List<Int>,
) : CustomType<Film>, Identifiable {

    @Hidden
    override var id: Int? = null

    override fun TypeDefinitionBuilder.definition(): TypeDefinition<Film> {
        return `object` {
            inferFromStructure()

            property("id") {
                id!!
            }
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private class Person(
    val name: String,
    val gender: String,

    @JsonProperty("skin_color")
    val skinColor: String,
    @JsonProperty("hair_color")
    val hairColor: String,
    @JsonProperty("eye_color")
    val eyeColor: String,

    val height: String,
    val mass: String,

    @Hidden
    val homeworld: Int,
) : CustomType<Person>, Identifiable {
    @Hidden
    override var id: Int? = null

    @Hidden
    var species: Int? = null

    override fun TypeDefinitionBuilder.definition(): TypeDefinition<Person> {
        return `object` {
            inferFromStructure()

            property("id") {
                id!!
            }

            relationship<Planet> {
                homeworld.toString()
            }

            optionalRelationship<Species> {
                species?.toString()
            }
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class Species(
    val classification: String,
    val name: String,
    val designation: String,
    @JsonProperty("eye_colors")
    val eyeColors: String,
    @JsonProperty("skin_colors")
    val skinColors: String,
    val language: String,
    @JsonProperty("hair_colors")
    val hairColors: String,
    @JsonProperty("average_lifespan")
    val averageLifespan: String,
    @JsonProperty("average_height")
    val averageHeight: String,
    @Hidden
    val people: List<Int>,
    @Hidden
    val homeworld: Int
) : CustomType<Species>, Identifiable {
    @Hidden
    override var id: Int? = null

    override fun TypeDefinitionBuilder.definition(): TypeDefinition<Species> {
        return `object` {
            inferFromStructure()

            property("id") {
                id!!
            }

            relationship<Planet>("planet") {
                homeworld.toString()
            }
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class Planet(
    val climate: String,
    val surfaceWater: String?,
    val name: String,
    val diameter: String,
    @JsonProperty("rotation_period")
    val rotationPeriod: String,
    val created: String,
    val terrain: String,
    val gravity: String,
    @JsonProperty("orbital_period")
    val orbitalPeriod: String,
    val population: String
) : CustomType<Planet>, Identifiable {
    @Hidden
    override var id: Int? = null

    override fun TypeDefinitionBuilder.definition(): TypeDefinition<Planet> {
        return `object` {
            inferFromStructure()

            property("id") {
                id!!
            }
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class Starship(
    val mglt: String?,
    @JsonProperty("starship_class")
    val starshipClass: String,
    @JsonProperty("hyperdrive_rating")
    val hyperdriveRating: String,
    @Hidden
    val pilots: List<Int>
) : CustomType<Starship>, Identifiable {
    @Hidden
    override var id: Int? = null

    override fun TypeDefinitionBuilder.definition(): TypeDefinition<Starship> {
        return `object` {
            inferFromStructure()

            property("id") {
                id!!
            }
        }
    }
}

private fun <A, B> Map<A, List<B>>.flip(): Map<B, List<A>> {
    val map = mutableMapOf<B, List<A>>()
    forEach { (key, values) ->
        values.forEach { value ->
            val previous = map[value] ?: emptyList()
            map[value] = previous + listOf(key)
        }
    }
    return map.toMap()
}