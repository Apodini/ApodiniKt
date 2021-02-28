package de.tum.`in`.ase.apodini.logging

interface LogLevels {
    val fatal: Logger.LogLevel
        get() = Logger.LogLevel.Fatal

    val error: Logger.LogLevel
        get() = Logger.LogLevel.Error

    val warning: Logger.LogLevel
        get() = Logger.LogLevel.Warning

    val info: Logger.LogLevel
        get() = Logger.LogLevel.Info

    val debug: Logger.LogLevel
        get() = Logger.LogLevel.Debug
}