package de.tum.`in`.ase.apodini.model

import de.tum.`in`.ase.apodini.Component
import de.tum.`in`.ase.apodini.ComponentBuilder
import de.tum.`in`.ase.apodini.Handler
import de.tum.`in`.ase.apodini.WebService
import de.tum.`in`.ase.apodini.configuration.ConfigurationBuilder
import de.tum.`in`.ase.apodini.environment.EnvironmentBuilder
import de.tum.`in`.ase.apodini.environment.EnvironmentStore
import de.tum.`in`.ase.apodini.environment.override
import de.tum.`in`.ase.apodini.exporter.Exporter
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
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

internal fun WebService.semanticModel(): SemanticModel {
    val configurationBuilder = StandardConfigurationBuilder()
    configurationBuilder.configure()

    val componentBuilder = ComponentBuilderCursor()
    componentBuilder()

    return SemanticModel(
        configurationBuilder.exporters,
        componentBuilder.endpoints,
        configurationBuilder.environmentStore
    )
}

private class StandardConfigurationBuilder : ConfigurationBuilder {
    val exporters = mutableListOf<Exporter>()
    var environmentStore = EnvironmentStore.empty
        private set

    override fun use(exporter: Exporter) {
        exporters.add(exporter)
    }

    override fun environment(init: EnvironmentBuilder.() -> Unit) {
        environmentStore = environmentStore.override(init)
    }
}

private class ComponentBuilderCursor : ComponentVisitor() {
    val endpoints = mutableListOf<SemanticModel.Endpoint>()
    private var current: StandardComponentBuilder = StandardComponentBuilder(this, emptyList())

    override fun enterGroup(kind: Group) {
        val path = when (kind) {
            Group.Environment -> TODO()
            is Group.Named -> SemanticModel.PathComponent.StringPathComponent(kind.name)
            is Group.Parameter -> SemanticModel.PathComponent.ParameterPathComponent(kind.parameter)
        }

        current = StandardComponentBuilder(this, current.path + path, current)
    }

    override fun exitGroup() {
        current = current.parent ?: current
    }

    override fun add(component: Component) {
        current.add(component)
    }

    override fun <T> add(handler: Handler<T>, returnType: KType) {
        current.add(handler, returnType)
    }
}

private class StandardComponentBuilder(
        val cursor: ComponentBuilderCursor,
        val path: List<SemanticModel.PathComponent>,
        val parent: StandardComponentBuilder? = null
) : ComponentBuilder() {
    public override fun add(component: Component) {
        if (component is InternalComponent) {
            val internal: InternalComponent = component
            with (internal) {
                cursor.visit()
            }
        } else {
            with(component) {
                cursor()
            }
        }
    }

    override fun <T> add(handler: Handler<T>, returnType: KType) {
        val parameters = ParameterCollector()
            .also { collector ->
                handler.traverse<RequestInjectable> { name, injectable ->
                    with(injectable) {
                        collector.collect(name)
                    }
                }
            }
            .build()

        val endpoint = SemanticModel.ConcreteEndpoint(
            path = path,
            typeDefinition = TypeDefinitionInferenceManager.infer(returnType),
            handler = handler,
            environment = EnvironmentStore.empty,
            parameters = parameters
        )

        cursor.endpoints.add(endpoint)
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
    val concreteLookedUpType = lookedUpType.classifier as KClass<*>

    for (field in type.declaredFields) {
        val wasAccessible = field.isAccessible
        field.isAccessible = true
        val value = field.get(this)
        if (concreteLookedUpType.isInstance(value)) {
            @Suppress("UNCHECKED_CAST")
            block(field.name.removeSuffix("\$delegate"), value as T)
        } else if (value is DynamicProperty) {
            value.traverse(lookedUpType, block)
        }
        field.isAccessible = wasAccessible
    }
}