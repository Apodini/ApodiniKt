package de.tum.`in`.ase.apodini.exporter

import de.tum.`in`.ase.apodini.model.SemanticModel

interface Exporter {
    fun export(model: SemanticModel)
}