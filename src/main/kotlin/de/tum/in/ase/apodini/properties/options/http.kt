package de.tum.`in`.ase.apodini.properties.options

import de.tum.`in`.ase.apodini.properties.Parameter

sealed class HTTPParameterMode<in T> {
    object Body : HTTPParameterMode<Any>()

    object Path : HTTPParameterMode<String>()
    object Query : HTTPParameterMode<String?>()
    object Header : HTTPParameterMode<String?>()

    companion object : OptionKey<Parameter<*>, HTTPParameterMode<*>> {
        val body = Body
        val path = Path
        val query = Query
        val header = Header
    }
}

fun <T> OptionsBuilder<Parameter<T>>.http(mode: HTTPParameterMode.Companion.() -> HTTPParameterMode<T>) {
    http(HTTPParameterMode.mode())
}

fun <T> OptionsBuilder<Parameter<T>>.http(mode: HTTPParameterMode<T>) {
    HTTPParameterMode to mode
}