package de.tum.`in`.ase.apodini.internal

import de.tum.`in`.ase.apodini.request.Request
import kotlin.reflect.KProperty

internal interface RequestInjectable {
    fun PropertyCollector.collect(property: KProperty<*>) = Unit
    fun inject(request: Request)
}