package de.tum.`in`.ase.apodini.internal

import de.tum.`in`.ase.apodini.request.Request

internal interface RequestInjectable {
    fun PropertyCollector.collect(propertyName: String) = Unit
    fun inject(request: Request)
}