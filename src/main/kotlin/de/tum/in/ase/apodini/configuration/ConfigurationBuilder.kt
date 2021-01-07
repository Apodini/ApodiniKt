package de.tum.`in`.ase.apodini.configuration

import de.tum.`in`.ase.apodini.exporter.Exporter

interface ConfigurationBuilder {
    fun use(exporter: Exporter)
    fun environment()
}