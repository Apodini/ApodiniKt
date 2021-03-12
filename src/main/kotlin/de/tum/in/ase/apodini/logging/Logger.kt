package de.tum.`in`.ase.apodini.logging

import de.tum.`in`.ase.apodini.environment.handler
import de.tum.`in`.ase.apodini.environment.request
import de.tum.`in`.ase.apodini.properties.environment

interface Logger {
    enum class LogLevel {
        Fatal, Error, Warning, Info, Debug
    }

    interface MessageFormatter {
        fun LogLevel.format(
            message: String
        ): String

        companion object {
            val default: MessageFormatter = DefaultFormatter
        }
    }

    operator fun LogLevel.invoke(message: () -> String)
    operator fun LogLevel.invoke(message: String) {
        this {
            message
        }
    }

    fun fatal(message: () -> String) {
        LogLevel.Fatal(message)
    }

    fun error(message: () -> String) {
        LogLevel.Error(message)
    }

    fun warning(message: () -> String) {
        LogLevel.Warning(message)
    }

    fun info(message: () -> String) {
        LogLevel.Info(message)
    }

    fun debug(message: () -> String) {
        LogLevel.Debug(message)
    }

    fun fatal(message: String) {
        LogLevel.Fatal(message)
    }

    fun error(message: String) {
        LogLevel.Error(message)
    }

    fun warning(message: String) {
        LogLevel.Warning(message)
    }

    fun info(message: String) {
        LogLevel.Info(message)
    }

    fun debug(message: String) {
        LogLevel.Debug(message)
    }

    companion object {
        val console: Logger = ConsoleLogger
    }
}

private object DefaultFormatter : Logger.MessageFormatter {
    private val handler by environment { handler }
    private val request by environment { request }

    override fun Logger.LogLevel.format(message: String): String {
        return "$name : $request : $handler : $message"
    }
}

private object ConsoleLogger : Logger {
    private val formatter by environment { preferredMessageFormatter }
    private val preferredLogLevel by environment { preferredLogLevel }

    override fun Logger.LogLevel.invoke(message: () -> String) {
        if (ordinal <= preferredLogLevel.ordinal) {
            with(formatter) {
                print(format(message()))
            }
        }
    }
}