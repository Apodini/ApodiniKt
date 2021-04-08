package de.tum.`in`.ase.apodini.test.example

import de.tum.`in`.ase.apodini.*
import de.tum.`in`.ase.apodini.configuration.ConfigurationBuilder
import de.tum.`in`.ase.apodini.environment.EnvironmentKey
import de.tum.`in`.ase.apodini.environment.EnvironmentKeys
import de.tum.`in`.ase.apodini.environment.request
import de.tum.`in`.ase.apodini.exporter.RESTExporter
import de.tum.`in`.ase.apodini.impl.text
import de.tum.`in`.ase.apodini.impl.group
import de.tum.`in`.ase.apodini.logging.logger
import de.tum.`in`.ase.apodini.model.operation
import de.tum.`in`.ase.apodini.modifiers.withEnvironment
import de.tum.`in`.ase.apodini.properties.*
import de.tum.`in`.ase.apodini.properties.options.default
import de.tum.`in`.ase.apodini.properties.options.http
import de.tum.`in`.ase.apodini.types.*
import kotlinx.coroutines.CoroutineScope
import java.lang.IllegalArgumentException

fun main() {
    TestWebService.run()
}

object TestWebService : WebService {
    private val userId = pathParameter()

    override fun ComponentBuilder.invoke() {
        text("Hello World")
        group("kotlin") {
            text("Hello, Kotlin")
            group("1.4") {
                text("Hello, Kotlin 1.4")
            }
        }

        group("me") {
            +CurrentlyAuthenticatedUser()
        }

        group("user", userId) {
            +GreeterForUser(userId).withEnvironment {
                secret {
                    0
                }
            }

            group("post") {
                +PostsForUser(userId)
            }
        }.withEnvironment {
            secret {
                1337
            }
        }

        group("greeting") {
            +Greeter()
        }

        group("message") {
            +MessageUpdater()
                .operation {
                    update
                }
        }
    }

    override fun ConfigurationBuilder.configure() {
        use(PrintExporter)
        use(RESTExporter(port = 8080))

        environment {
            secret {
                42
            }
        }
    }
}

// MARK: Authentication

enum class UserRole {
    Individual, Moderator, Admin
}

data class URL(val urlString: String) : CustomType<URL> {
    override fun TypeDefinitionBuilder.definition() = string<URL> { urlString }
}

@Documented("Represents a user")
data class User(
    val name: String,
    val age: Int,
    val homepage: URL,
    val role: UserRole,
    val aliases: List<String>
) : CustomType<User> {

    @Hidden
    val privateAttribute: Int = 42

    override fun TypeDefinitionBuilder.definition() = `object`<User> {
        inferFromStructure()

        property("upperCaseName") {
            name.toUpperCase()
        }
    }

    companion object : BasicAuthenticationUserFactory<User> {
        override suspend fun user(username: String, password: String): User {
            if (username == "test@example.org" && password == "password") {
                return User(
                    "Mathias",
                    25,
                    URL("https://quintero.io"),
                    UserRole.Admin,
                    listOf("Quincy")
                )
            }

            throw IllegalArgumentException("Wrong Username and Password")
        }
    }
}

class CurrentlyAuthenticatedUser : Handler<User?> {
    private val authenticated by authenticated(User)
    private val logger by environment { logger }

    override suspend fun CoroutineScope.compute(): User? {
        if (authenticated == null) {
            logger.debug("User is not authenticated")
        }
        return authenticated
    }
}

// MARK: Path Parameters

class GreeterForUser(id: PathParameter) : Handler<String> {
    private val id by id
    private val name by parameter<String?>()

    private val request by environment { request }
    private val logger by environment { logger }

    override suspend fun CoroutineScope.compute(): String {
        logger.info("Trying out logging")
        logger.debug {
            "Received id $id from $request"
        }

        val name = name ?: return "Hello, User $id"
        return "Hello, $name (User $id)"
    }
}

class PostsForUser(id: PathParameter) : Handler<String> {
    private val id by id

    override suspend fun CoroutineScope.compute(): String {
        return "Posts from user $id"
    }
}

// MARK: Custom Environment Key + query parameters

class Greeter: Handler<String> {
    private val logger by environment { logger }
    private val secret by environment { secret }

    private val name: String by parameter {
        http {
            query
        }
        default("World")
    }

    override suspend fun CoroutineScope.compute(): String {
        logger.debug("Received Secret: $secret")
        return "Hello, $name"
    }
}

private var message = "Hello, World"
class MessageUpdater : Handler<String> {
    private val newValue by parameter<String>()

    override suspend fun CoroutineScope.compute(): String {
        return message.also { message = newValue }
    }
}

private object SecretKey : EnvironmentKey<Int>() {
    override val default = 0
}

val EnvironmentKeys.secret: EnvironmentKey<Int> get() = SecretKey