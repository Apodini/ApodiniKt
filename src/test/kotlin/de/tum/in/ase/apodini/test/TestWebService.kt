package de.tum.`in`.ase.apodini.test

import de.tum.`in`.ase.apodini.ComponentBuilder
import de.tum.`in`.ase.apodini.StringHandler
import de.tum.`in`.ase.apodini.WebService
import de.tum.`in`.ase.apodini.impl.text
import de.tum.`in`.ase.apodini.internal.group
import de.tum.`in`.ase.apodini.properties.parameter
import kotlin.coroutines.CoroutineContext

class TestWebService : WebService {
    override fun ComponentBuilder.invoke() {
        text("Hello World")
        group("kotlin") {
            text("Hello, Kotlin")
            group("1.4") {
                text("Hello, Kotlin 1.4")
            }
        }

        group("greeting") {
            +Greeter()
        }
    }
}

class Greeter: StringHandler {
    private val name: String by parameter()

    override suspend fun CoroutineContext.impl(): String {
        return "Hello, $name"
    }
}