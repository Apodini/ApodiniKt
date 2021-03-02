package de.tum.`in`.ase.apodini.test

import de.tum.`in`.ase.apodini.ComponentBuilder
import de.tum.`in`.ase.apodini.Handler
import de.tum.`in`.ase.apodini.WebService
import de.tum.`in`.ase.apodini.configuration.ConfigurationBuilder
import de.tum.`in`.ase.apodini.environment.EnvironmentKey
import de.tum.`in`.ase.apodini.environment.EnvironmentKeys
import de.tum.`in`.ase.apodini.environment.request
import de.tum.`in`.ase.apodini.exporter.RESTExporter
import de.tum.`in`.ase.apodini.impl.text
import de.tum.`in`.ase.apodini.impl.group
import de.tum.`in`.ase.apodini.logging.logger
import de.tum.`in`.ase.apodini.properties.*
import de.tum.`in`.ase.apodini.properties.options.default
import de.tum.`in`.ase.apodini.properties.options.http
import java.lang.IllegalArgumentException
import kotlin.coroutines.CoroutineContext

object TestWebService : WebService {
    private val userId = pathParameter()

    override fun ComponentBuilder.invoke() {
//        text("Hello World")
//        group("kotlin") {
//            text("Hello, Kotlin")
//            group("1.4") {
//                text("Hello, Kotlin 1.4")
//            }
//        }
//
//        group("me") {
//            +CurrentlyAuthenticatedUser()
//        }
//
//        group("user", userId) {
//            +GreeterForUser(userId)
//
//            group("post") {
//                +PostsForUser(userId)
//            }
//        }

        group("greeting") {
            +Greeter()
        }
    }

    override fun ConfigurationBuilder.configure() {
        use(RESTExporter())

        environment {
            secret {
                42
            }
        }
    }
}

// MARK: Authentication

data class User(val name: String, val age: Int) {
    companion object : BasicAuthenticationUserFactory<User> {
        override suspend fun user(username: String, password: String): User {
            if (username == "test@example.org" && password == "password") {
                return User("Mathias", 25)
            }

            throw IllegalArgumentException("Wrong Username and Password")
        }
    }
}

class CurrentlyAuthenticatedUser : Handler<User?> {
    private val authenticated by authenticated(User)
    private val logger by environment { logger }

    override suspend fun CoroutineContext.compute(): User? {
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

    override suspend fun CoroutineContext.compute(): String {
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

    override suspend fun CoroutineContext.compute(): String {
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

    override suspend fun CoroutineContext.compute(): String {
        logger.debug("Received Secret: $secret")
        return "Hello, $name"
    }
}

private object SecretKey : EnvironmentKey<Int>() {
    override val default = 0
}

val EnvironmentKeys.secret: EnvironmentKey<Int> get() = SecretKey