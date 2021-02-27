package de.tum.`in`.ase.apodini.properties

import de.tum.`in`.ase.apodini.properties.options.http
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.reflect.KProperty

interface BearerAccessTokenUserFactory<T> {
    suspend fun user(accessToken: String): T
}

interface BasicAuthenticationUserFactory<T> {
    suspend fun user(username: String, password: String): T
}

fun <T : Any> authenticated(
        factory: BearerAccessTokenUserFactory<T>
): Authenticated<T> {
    return Authenticated(Authenticated.Factory.Bearer(factory))
}

fun <T : Any> authenticated(
        factory: BasicAuthenticationUserFactory<T>
): Authenticated<T> {
    return Authenticated(Authenticated.Factory.Basic(factory))
}

class Authenticated<T : Any> internal constructor(
        private val factory: Factory<T>,
): DynamicProperty {
    internal sealed class Factory<T> {
        data class Bearer<T>(val value: BearerAccessTokenUserFactory<T>) : Factory<T>()
        data class Basic<T>(val value: BasicAuthenticationUserFactory<T>) : Factory<T>()

        suspend fun user(authorizationHeader: String): T? {
            when (this) {
                is Bearer -> {
                    if (authorizationHeader.startsWith("Bearer ", true)) {
                        val accessToken = authorizationHeader.removePrefix("Bearer ")
                        return value.user(accessToken)
                    }
                    return null
                }
                is Basic -> {
                    if (authorizationHeader.startsWith("Basic ", true)) {
                        val base64 = authorizationHeader.removePrefix("Basic ")
                        val decoded = Base64.getDecoder().decode(base64)
                        val values = String(decoded).split(":").filter { it.isNotEmpty() }
                        if (values.count() != 2) {
                            throw IllegalArgumentException("Invalid Basic Authorization Header")
                        }
                        return value.user(values[0], values[1])
                    }
                    return null
                }
            }
        }
    }

    private val authorization by parameter<String?> {
        http {
            header
        }
    }

    private var user: T? = null

    override suspend fun update() {
        user = authorization?.let { factory.user(it) }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return user
    }
}