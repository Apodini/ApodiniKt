package de.tum.`in`.ase.apodini

import de.tum.`in`.ase.apodini.configuration.ConfigurationBuilder
import de.tum.`in`.ase.apodini.exporter.REST
import de.tum.`in`.ase.apodini.model.semanticModel

interface WebService: Component {
    fun ConfigurationBuilder.configure() {
        use(REST())
    }
}

fun WebService.run() {
    val model = semanticModel()
    model.exporters.forEach { it.export(model) }
    // TODO: Finish exporting by blocking
}