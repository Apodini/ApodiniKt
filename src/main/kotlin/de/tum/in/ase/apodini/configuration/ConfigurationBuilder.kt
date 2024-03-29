package de.tum.`in`.ase.apodini.configuration

import de.tum.`in`.ase.apodini.environment.EnvironmentBuilder
import de.tum.`in`.ase.apodini.exporter.Exporter

@ConfigurationDsl
interface ConfigurationBuilder {
    fun use(exporter: Exporter)
    fun environment(init: EnvironmentBuilder.() -> Unit)
}