package de.tum.`in`.ase.apodini.test

import de.tum.`in`.ase.apodini.Component
import de.tum.`in`.ase.apodini.ComponentBuilder
import de.tum.`in`.ase.apodini.WebService
import de.tum.`in`.ase.apodini.model.SemanticModel
import de.tum.`in`.ase.apodini.model.semanticModel


internal fun semanticModel(init: ComponentBuilder.() -> Unit): SemanticModel {
    return WrapperService(init).semanticModel()
}

private class WrapperService(init: ComponentBuilder.() -> Unit) : WebService, Component by Component(init)