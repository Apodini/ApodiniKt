package de.tum.`in`.ase.apodini.logging

import de.tum.`in`.ase.apodini.environment.EnvironmentKey
import de.tum.`in`.ase.apodini.environment.EnvironmentKeys
import de.tum.`in`.ase.apodini.properties.environment
import de.tum.`in`.ase.apodini.request.Request

private object PreferredLogLevelKey : EnvironmentKey<Logger.LogLevel>() {
    override val default = Logger.LogLevel.Fatal
}

private object LoggerEnvironmentKey : EnvironmentKey<Logger>() {
    override val default = Logger.console
}

val EnvironmentKeys.logger: EnvironmentKey<Logger>
    get() = LoggerEnvironmentKey

val EnvironmentKeys.preferredLogLevel: EnvironmentKey<Logger.LogLevel>
    get() = PreferredLogLevelKey


private object LoggerMessageFormatterEnvironmentKey : EnvironmentKey<Logger.MessageFormatter>() {
    override val default = Logger.MessageFormatter.default
}

val EnvironmentKeys.preferredMessageFormatter: EnvironmentKey<Logger.MessageFormatter>
    get() = LoggerMessageFormatterEnvironmentKey

internal val Request.logger: Logger
    get() {
        val loggerEnvironment = environment { logger }
        loggerEnvironment.inject(this)
        val logger by loggerEnvironment
        return logger
    }