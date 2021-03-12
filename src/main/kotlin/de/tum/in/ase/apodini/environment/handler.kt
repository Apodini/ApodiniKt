package de.tum.`in`.ase.apodini.environment

import de.tum.`in`.ase.apodini.Handler

private object HandlerKey : EnvironmentKey<Handler<*>>() {
    override val default: Handler<*>
        get() = throw IllegalArgumentException("Handler not provided!")
}

val EnvironmentKeys.handler: EnvironmentKey<Handler<*>>
    get() = HandlerKey