package de.tum.`in`.ase.apodini

import de.tum.`in`.ase.apodini.configuration.ConfigurationBuilder
import de.tum.`in`.ase.apodini.exporter.RESTExporter

interface WebService: Component {
    fun ConfigurationBuilder.configure() {
        use(RESTExporter())
    }
}