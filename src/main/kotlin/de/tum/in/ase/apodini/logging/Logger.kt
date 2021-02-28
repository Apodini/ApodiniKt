package de.tum.`in`.ase.apodini.logging

interface Logger : LogLevels {
    enum class LogLevel {
        Fatal, Error, Warning, Info, Debug
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
}
