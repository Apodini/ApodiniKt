package de.tum.`in`.ase.apodini.test

import de.tum.`in`.ase.apodini.ComponentBuilder
import de.tum.`in`.ase.apodini.Handler
import de.tum.`in`.ase.apodini.WebService
import de.tum.`in`.ase.apodini.impl.text
import de.tum.`in`.ase.apodini.impl.group
import de.tum.`in`.ase.apodini.properties.PathParameter
import de.tum.`in`.ase.apodini.properties.options.http
import de.tum.`in`.ase.apodini.properties.parameter
import de.tum.`in`.ase.apodini.properties.pathParameter
import kotlin.coroutines.CoroutineContext

class TestWebService : WebService {
    private val id = pathParameter<String>()

    override fun ComponentBuilder.invoke() {
        text("Hello World")
        group("kotlin") {
            text("Hello, Kotlin")
            group("1.4") {
                text("Hello, Kotlin 1.4")
            }
        }

        group("user", id) {
            GreeterForUser(id)
        }

        group("greeting") {
            +Greeter()
        }
    }
}

class GreeterForUser(id: PathParameter<String>) : Handler<String> {
    private val id by id

    override suspend fun CoroutineContext.compute(): String {
        return "Hello, User $id"
    }
}

class Greeter: Handler<String> {
    private val name: String by parameter {
        http {
            body
        }
    }

    override suspend fun CoroutineContext.compute(): String {
        return "Hello, $name"
    }
}