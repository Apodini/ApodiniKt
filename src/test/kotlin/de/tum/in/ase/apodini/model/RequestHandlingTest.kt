package de.tum.`in`.ase.apodini.model

import de.tum.`in`.ase.apodini.Handler
import de.tum.`in`.ase.apodini.impl.group
import de.tum.`in`.ase.apodini.impl.text
import de.tum.`in`.ase.apodini.logging.Logger
import de.tum.`in`.ase.apodini.logging.logger
import de.tum.`in`.ase.apodini.modifiers.withEnvironment
import de.tum.`in`.ase.apodini.properties.environment
import de.tum.`in`.ase.apodini.properties.parameter
import de.tum.`in`.ase.apodini.test.handle
import de.tum.`in`.ase.apodini.test.secret
import junit.framework.TestCase
import kotlinx.coroutines.CoroutineScope
import java.lang.IllegalArgumentException
import kotlin.math.log
import kotlin.test.assertFailsWith

class RequestHandlingTest : TestCase() {

    fun testText() {
        val result = handle {
            text("Hello, World!")
        }

        assertEquals(result, "Hello, World!")
    }

    fun testObject() {
        val result = handle {
            +object : Handler<Foo> {
                override suspend fun CoroutineScope.compute(): Foo {
                    return Foo("Test", 42)
                }
            }
        }

        assertEquals(result, mapOf("bar" to "Test", "baz" to 42))
    }

    fun testArray() {
        val result = handle {
            +object : Handler<List<Int>> {
                override suspend fun CoroutineScope.compute(): List<Int> {
                    return listOf(1, 2, 3)
                }
            }
        }

        assertEquals(result, listOf(1, 2, 3))
    }

    fun testHandleParameter() {
        val result = handle("name" to "World") {
            +object : Handler<String> {
                val name by parameter<String>()

                override suspend fun CoroutineScope.compute(): String {
                    return "Hello, $name!"
                }
            }
        }

        assertEquals(result, "Hello, World!")
    }

    fun testHandleEnvironment() {
        val result = handle {
            group("test") {
                +object : Handler<String> {
                    val someSecret by environment { secret }

                    override suspend fun CoroutineScope.compute(): String {
                        return "Secret = $someSecret. Don't tell anyone"
                    }
                }
            }.withEnvironment { secret { 42 } }
        }

        assertEquals(result, "Secret = 42. Don't tell anyone")
    }

    fun testThrowingAnExceptionIsPropagated() {
        val logger = SomeLogger()
        assertFailsWith<IllegalArgumentException> {
            handle {
                +object : Handler<String> {
                    override suspend fun CoroutineScope.compute(): String {
                        throw IllegalArgumentException("Nope")
                    }
                }.withEnvironment { logger { logger } }
            }
        }

        // Check that it gets logged
        assertEquals(logger.messages.count(), 1)
    }

}

private class SomeLogger : Logger {
    val messages = mutableListOf<String>()

    override fun Logger.LogLevel.invoke(message: () -> String) {
        messages.add(message())
    }
}