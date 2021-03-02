package de.tum.`in`.ase.apodini.model

import de.tum.`in`.ase.apodini.Component
import de.tum.`in`.ase.apodini.ComponentBuilder
import de.tum.`in`.ase.apodini.Handler
import de.tum.`in`.ase.apodini.environment.EnvironmentStore
import de.tum.`in`.ase.apodini.internal.ComponentVisitor
import de.tum.`in`.ase.apodini.internal.InternalComponent
import de.tum.`in`.ase.apodini.internal.PropertyCollector
import de.tum.`in`.ase.apodini.internal.RequestInjectable
import de.tum.`in`.ase.apodini.properties.DynamicProperty
import de.tum.`in`.ase.apodini.properties.Parameter
import de.tum.`in`.ase.apodini.properties.options.OptionSet
import de.tum.`in`.ase.apodini.properties.options.default
import de.tum.`in`.ase.apodini.types.TypeDefinitionInferenceManager
import java.util.*
import kotlin.reflect.KType
import kotlin.reflect.typeOf

internal class SemanticModelBuilder {
    val endpoints = mutableListOf<SemanticModel.Endpoint>()
}

private class ComponentBuilderCursor(
        val builder: SemanticModelBuilder
) : ComponentVisitor {
    private var current: StandardComponentBuilder = StandardComponentBuilder(this, emptyList())

    override fun enterGroup(kind: ComponentVisitor.Group) {
        TODO("Not yet implemented")
    }

    override fun exitGroup() {
        TODO("Not yet implemented")
    }

    override fun Component.unaryPlus() {
        with(current) {
            +this@unaryPlus
        }
    }

    override fun <T> Handler<T>.unaryPlus() {
        with(current) {
            +this@unaryPlus
        }
    }
}

private class StandardComponentBuilder(
        val cursor: ComponentBuilderCursor,
        val path: List<SemanticModel.PathComponent>
) : ComponentBuilder {
    override fun Component.unaryPlus() {
        if (this is InternalComponent) {
            val internal: InternalComponent = this
            with (internal) {
                cursor.visit()
            }
        } else {
            this@StandardComponentBuilder()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun <T> Handler<T>.unaryPlus() {
        val parameters = ParameterCollector()
                .also { collector ->
                    this@unaryPlus.traverse<RequestInjectable> { name, injectable ->
                        with(injectable) {
                            collector.collect(name)
                        }
                    }
                }
                .build()

        val endpoint = SemanticModel.ConcreteEndpoint<T>(
                path = path,
                typeDefinition = TypeDefinitionInferenceManager.infer(typeOf<String>()),
                handler = this,
                environment = EnvironmentStore.empty,
                parameters = parameters
        )

        cursor.builder.endpoints.add(endpoint)
    }
}

private class ParameterCollector : PropertyCollector {
    private val parameters = mutableListOf<SemanticModel.Parameter<*>>()

    override fun <T> registerParameter(id: UUID, name: String, type: KType, options: OptionSet<Parameter<T>>) {
        val defaultValue = options<T> { default }
        parameters.add(SemanticModel.Parameter(id, name, type, defaultValue, options))
    }

    fun build(): List<SemanticModel.Parameter<*>> {
        return parameters
    }
}

@OptIn(ExperimentalStdlibApi::class)
private inline fun <reified T> Any.traverse(noinline block: (String, T) -> Unit) {
    traverse(typeOf<T>(), block)
}

private fun <T> Any.traverse(lookedUpType: KType, block: (String, T) -> Unit) {
    val type = this::class.java

    for (field in type.fields) {
        val wasAccessible = field.isAccessible
        field.isAccessible = true
        when (val value = field.get(this)) {
            type.kotlin.isInstance(value) -> {
                @Suppress("UNCHECKED_CAST")
                block(field.name, value as T)
            }
            is DynamicProperty -> {
                value.traverse(lookedUpType, block)
            }
            else -> {}
        }
        field.isAccessible = wasAccessible
    }
}