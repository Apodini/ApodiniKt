package de.tum.`in`.ase.apodini.logging

import de.tum.`in`.ase.apodini.environment.EnvironmentBuilder
import de.tum.`in`.ase.apodini.environment.EnvironmentKey
import de.tum.`in`.ase.apodini.environment.EnvironmentKeys
import de.tum.`in`.ase.apodini.properties.environment
import de.tum.`in`.ase.apodini.request.Request
import sun.rmi.runtime.Log

private object PreferredLogLevelKey : EnvironmentKey<Logger.LogLevelPreference>() {
    override val default = Logger.LogLevelPreference.Off
}

private object LoggerEnvironmentKey : EnvironmentKey<Logger>() {
    override val default = Logger.console
}

private object LoggerMessageFormatterEnvironmentKey : EnvironmentKey<Logger.MessageFormatter>() {
    override val default = Logger.MessageFormatter.default
}

val EnvironmentKeys.logger: EnvironmentKey<Logger>
    get() = LoggerEnvironmentKey

val EnvironmentKeys.preferredLogLevel: EnvironmentKey<Logger.LogLevelPreference>
    get() = PreferredLogLevelKey

val EnvironmentKeys.preferredMessageFormatter: EnvironmentKey<Logger.MessageFormatter>
    get() = LoggerMessageFormatterEnvironmentKey

fun EnvironmentBuilder.preferredLogLevel(level: Logger.LogLevel) {
    preferredLogLevel {
        Logger.LogLevelPreference.Minimum(level)
    }
}

internal val Request.logger: Logger
    get() {
        val loggerEnvironment = environment { logger }
        loggerEnvironment.inject(this)
        val logger by loggerEnvironment
        return logger
    }