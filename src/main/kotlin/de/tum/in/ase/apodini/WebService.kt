package de.tum.`in`.ase.apodini

import de.tum.`in`.ase.apodini.configuration.ConfigurationBuilder
import de.tum.`in`.ase.apodini.exporter.RESTExporter
import de.tum.`in`.ase.apodini.model.SemanticModel
import de.tum.`in`.ase.apodini.model.semanticModel

interface WebService: Component {
    fun ConfigurationBuilder.configure() {
        use(RESTExporter())
    }
}

fun WebService.run() {
    val model = semanticModel()
    model.exporters.forEach { it.export(model) }
    // TODO: Finish exporting by blocking
}