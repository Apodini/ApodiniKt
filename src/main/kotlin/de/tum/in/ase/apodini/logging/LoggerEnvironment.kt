package de.tum.`in`.ase.apodini.logging

import de.tum.`in`.ase.apodini.environment.EnvironmentKey
import de.tum.`in`.ase.apodini.environment.EnvironmentKeys

val EnvironmentKeys.logger: EnvironmentKey<Logger>
    get() = LoggerEnvironmentKey

private object LoggerEnvironmentKey : EnvironmentKey<Logger>() {
    override val default = NoopLogger
}

private object NoopLogger : Logger {
    override fun Logger.LogLevel.invoke(message: () -> String) {
        // No-op
    }
}