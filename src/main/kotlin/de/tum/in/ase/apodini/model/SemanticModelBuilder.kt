package de.tum.`in`.ase.apodini.model

import de.tum.`in`.ase.apodini.*
import de.tum.`in`.ase.apodini.configuration.ConfigurationBuilder
import de.tum.`in`.ase.apodini.environment.EnvironmentBuilder
import de.tum.`in`.ase.apodini.environment.EnvironmentStore
import de.tum.`in`.ase.apodini.environment.extend
import de.tum.`in`.ase.apodini.environment.override
import de.tum.`in`.ase.apodini.exporter.Exporter
import de.tum.`in`.ase.apodini.exporter.httpOption
import de.tum.`in`.ase.apodini.internal.*
import de.tum.`in`.ase.apodini.internal.ComponentVisitor
import de.tum.`in`.ase.apodini.internal.InternalComponent
import de.tum.`in`.ase.apodini.internal.ModifiedComponent
import de.tum.`in`.ase.apodini.internal.PropertyCollector
import de.tum.`in`.ase.apodini.internal.RequestInjectable
import de.tum.`in`.ase.apodini.properties.Parameter
import de.tum.`in`.ase.apodini.properties.options.OptionSet
import de.tum.`in`.ase.apodini.properties.options.default
import de.tum.`in`.ase.apodini.internal.reflection.TypeDefinitionInferenceManager
import de.tum.`in`.ase.apodini.internal.reflection.traverse
import de.tum.`in`.ase.apodini.modifiers.ModifiableComponent
import de.tum.`in`.ase.apodini.modifiers.Modifier
import de.tum.`in`.ase.apodini.properties.PathParameter
import de.tum.`in`.ase.apodini.properties.options.HTTPParameterMode
import de.tum.`in`.ase.apodini.types.Documented
import java.util.*
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation

internal fun WebService.semanticModel(): SemanticModel {
    val configurationBuilder = StandardConfigurationBuilder()
    configurationBuilder.configure()

    return SemanticModel(
        configurationBuilder.exporters,
        configurationBuilder.environmentStore
    ).also { semanticModel ->
        val componentBuilder = ComponentBuilderCursor()
        componentBuilder()
        with(componentBuilder) {
            semanticModel.inject()
        }
    }
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

private interface InternalModifiableComponent : ModifiableComponent<InternalModifiableComponent> {
    fun SemanticModel.inject()
}

private class ComponentBuilderCursor(
    initialPath: List<SemanticModel.PathComponent> = emptyList(),
    initialEnvironment: EnvironmentStore = EnvironmentStore.empty,
    val inferenceManager: TypeDefinitionInferenceManager = TypeDefinitionInferenceManager()
) : ComponentVisitor() {
    val components = mutableListOf<InternalModifiableComponent>()
    private var current: StandardComponentBuilder = StandardComponentBuilder(this, initialPath, initialEnvironment)

    override fun enterGroup(kind: Group) {
        val environment = when (kind) {
            is Group.Environment -> kind.store.extend(current.environment)
            else -> current.environment
        }

        val path = when (kind) {
            is Group.Environment -> null
            is Group.Named -> SemanticModel.PathComponent.StringPathComponent(kind.name)
            is Group.Parameter -> SemanticModel.PathComponent.ParameterPathComponent(kind.parameter.asSemanticModelParameter())
        }

        val pathList = listOfNotNull(path)

        current = StandardComponentBuilder(this, current.path + pathList, environment, current)
    }

    override fun exitGroup() {
        current = current.parent ?: current
    }

    override fun add(component: Component): ModifiableComponent<*> {
        return current.add(component)
    }

    override fun <T> add(handler: Handler<T>, returnType: KType): ModifiableComponent<*> {
        return current.add(handler, returnType)
    }

    fun SemanticModel.inject() {
        components.forEach { component ->
            with (component) {
                inject()
            }
        }
    }
}

private class StandardComponentBuilder(
    private val cursor: ComponentBuilderCursor,
    val path: List<SemanticModel.PathComponent>,
    val environment: EnvironmentStore,
    val parent: StandardComponentBuilder? = null
) : ComponentBuilder() {
    public override fun add(component: Component) : ModifiableComponent<*> {
        return ComponentModifiableComponent(path, environment, component, cursor.inferenceManager).also { cursor.components.add(it) }
    }

    override fun <T> add(handler: Handler<T>, returnType: KType): ModifiableComponent<*> {
        if (handler is ModifiedHandler<T>) {
            return HandlerModifiableComponent(
                path,
                environment,
                handler.handler,
                returnType,
                cursor.inferenceManager,
                handler.modifiers
            ).also { cursor.components.add(it) }
        }

        return HandlerModifiableComponent(
            path,
            environment,
            handler,
            returnType,
            cursor.inferenceManager
        ).also { cursor.components.add(it) }
    }
}


private class ComponentModifiableComponent(
    private val path: List<SemanticModel.PathComponent>,
    private val environment: EnvironmentStore,
    private val component: Component,
    private val inferenceManager: TypeDefinitionInferenceManager
) : InternalModifiableComponent {
    private val modifiers = mutableListOf<Modifier>()

    override fun modify(modifier: Modifier): InternalModifiableComponent {
        modifiers.add(modifier)
        return this
    }

    override fun SemanticModel.inject() {
        val cursor = ComponentBuilderCursor(path, environment, inferenceManager)
        val modified = modifiers.fold(component) { acc, modifier -> ModifiedComponent(acc, modifier) }

        if (modified is InternalComponent) {
            val internal: InternalComponent = modified
            with (internal) {
                cursor.visit()
            }
        } else {
            with(modified) {
                cursor()
            }
        }

        with(cursor) {
            inject()
        }
    }
}

private class HandlerModifiableComponent<T>(
    private val path: List<SemanticModel.PathComponent>,
    private val environment: EnvironmentStore,
    private val handler: Handler<T>,
    private val returnType: KType,
    private val inferenceManager: TypeDefinitionInferenceManager,
    modifiers: List<Modifier> = emptyList()
) : InternalModifiableComponent {
    private val modifiers = mutableListOf(*(modifiers.toTypedArray()))

    override fun modify(modifier: Modifier): InternalModifiableComponent {
        modifiers.add(modifier)
        return this
    }

    override fun SemanticModel.inject() {
        if (modifiers.isNotEmpty()) {
            val cursor = ComponentBuilderCursor(path, environment, inferenceManager)
            val modified = modifiers.fold(HandlerWrapper(handler, returnType) as Component) { acc, modifier ->
                ModifiedComponent(acc, modifier)
            }

            with(modified) {
                cursor()
            }

            with(cursor) {
                inject()
            }

            return
        }

        val documentation = handler::class.findAnnotation<Documented>()?.documentation

        val parameters = ParameterCollector()
            .also { collector ->
                handler.traverse<RequestInjectable> { name, injectable ->
                    with(injectable) {
                        collector.collect(name)
                    }
                }
            }
            .build()

        val path = path.map { component ->
            when (component) {
                is SemanticModel.PathComponent.StringPathComponent -> component
                is SemanticModel.PathComponent.ParameterPathComponent -> SemanticModel.PathComponent.ParameterPathComponent(
                    parameters
                        .firstOrNull { it.id == component.parameter.id }
                        ?.let { param ->
                            @Suppress("UNCHECKED_CAST")
                            param as SemanticModel.Parameter<String>
                        }
                        ?: component.parameter
                )
            }
        }

        val setOfAlreadyUsedParameters = path
            .mapNotNull { it as? SemanticModel.PathComponent.ParameterPathComponent }
            .map { it.parameter.id }
            .toSet()

        val newParameters = parameters
            .filter { !setOfAlreadyUsedParameters.contains(it.id) && it.httpOption == HTTPParameterMode.path }
            .map { parameter ->
                @Suppress("UNCHECKED_CAST")
                SemanticModel.PathComponent.ParameterPathComponent(
                    parameter as SemanticModel.Parameter<String>
                )
            }

        val endpoint = SemanticModel.Endpoint(
            semanticModel = this,
            path = path + newParameters,
            typeDefinition = inferenceManager.infer(returnType),
            handler = handler,
            documentation = documentation,
            environment = environment,
            parameters = parameters
        )

        internalEndpoints.add(endpoint)
    }
}

private fun PathParameter.asSemanticModelParameter(): SemanticModel.Parameter<String> {
    @Suppress("UNCHECKED_CAST")
    return ParameterCollector()
        .also { collector ->
            this.traverse<RequestInjectable> { name, injectable ->
                with(injectable) {
                    collector.collect(name)
                }
            }
        }
        .build()
        .first() as SemanticModel.Parameter<String>
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