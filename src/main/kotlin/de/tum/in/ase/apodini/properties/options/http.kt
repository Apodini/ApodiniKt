package de.tum.`in`.ase.apodini.properties.options

import de.tum.`in`.ase.apodini.properties.Parameter

enum class HTTPParameterMode {
    Body, Path, Query, Header;

    companion object {
        val body = Body
        val path = Path
        val query = Query
        val header = Header
    }
}

fun OptionsBuilder<Parameter<*>>.http(mode: HTTPParameterMode.Companion.() -> HTTPParameterMode) {
    HTTPParameterModeOptionKey to HTTPParameterMode.mode()
}

object HTTPParameterModeOptionKey : OptionKey<Parameter<*>, HTTPParameterMode>