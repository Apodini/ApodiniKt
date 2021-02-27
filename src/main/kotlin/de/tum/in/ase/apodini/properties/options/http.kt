package de.tum.`in`.ase.apodini.properties.options

import de.tum.`in`.ase.apodini.properties.Parameter

enum class HTTPParameterMode {
    Body, Path, Query, Header;

    companion object : OptionKey<Parameter<*>, HTTPParameterMode> {
        val body = Body
        val path = Path
        val query = Query
        val header = Header
    }
}

fun OptionsBuilder<Parameter<*>>.http(mode: HTTPParameterMode.Companion.() -> HTTPParameterMode) {
    http(HTTPParameterMode.mode())
}

fun OptionsBuilder<Parameter<*>>.http(mode: HTTPParameterMode) {
    HTTPParameterMode to mode
}