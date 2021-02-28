package de.tum.`in`.ase.apodini.environment

import de.tum.`in`.ase.apodini.request.Request

private object RequestKey : EnvironmentKey<Request>() {
    override val default: Request
        get() = throw IllegalArgumentException("Request not provided!")
}

val EnvironmentKeys.request: EnvironmentKey<Request>
    get() = RequestKey