package de.tum.`in`.ase.apodini.test

import de.tum.`in`.ase.apodini.exporter.Exporter
import de.tum.`in`.ase.apodini.model.SemanticModel
import de.tum.`in`.ase.apodini.types.*
import de.tum.`in`.ase.apodini.types.Array
import de.tum.`in`.ase.apodini.types.Enum

object PrintExporter : Exporter {
    override fun export(model: SemanticModel) {
        print {
            separated {
                appendLine("Exporting Web Service")
                appendLine("Exporters")
                indent {
                    model.exporters.forEach { exporter ->
                        appendLine("$exporter")
                    }
                }
            }

            appendLine()
            model.endpoints.forEach { endpoint ->
                endpoint(endpoint)
            }
        }
    }

    private fun StringBuilder.endpoint(endpoint: SemanticModel.Endpoint<*>) {
        separated {
            if (endpoint.path.isEmpty()) {
                appendLine("/")
            } else {
                path(endpoint.path)
            }

            appendLine()
            append("Returns: ")
            type(endpoint.typeDefinition)
        }
    }

    private fun StringBuilder.type(definition: TypeDefinition<*>) {
        when (definition) {
            is Array<*> -> {
                appendLine("Array {")
                indent {
                    type(definition.definition)
                }
                appendLine("}")
            }
            is Enum -> {
                definition.documentation?.let { documentation ->
                    appendLine("# $documentation")
                }
                appendLine("enum ${definition.name} {")
                indent {
                    definition.cases.forEach { case ->
                        appendLine(case)
                    }
                }
                appendLine("}")
            }
            is Nullable -> {
                appendLine("Nullable {")
                indent {
                    type(definition.definition)
                }
                appendLine("}")
            }
            is Object -> {
                definition.documentation?.let { documentation ->
                    appendLine("# $documentation")
                }
                appendLine("type ${definition.name} {")
                indent {
                    definition.properties.forEach { property ->
                        property.documentation?.let { documentation ->
                            appendLine("# $documentation")
                        }
                        append(property.name)
                        append(": ")
                        type(property.definition)
                    }
                }
                appendLine("}")
            }
            is Scalar<*, *> -> {
                definition.documentation?.let { documentation ->
                    appendLine("# $documentation")
                }
                if (definition.name == definition.kind.name) {
                    appendLine("scalar ${definition.name}")
                } else {
                    appendLine("scalar ${definition.name} : ${definition.kind.name}")
                }
            }
            is ScalarType<*> -> {
                appendLine(definition.name)
            }
        }
    }

    private fun StringBuilder.path(path: Iterable<SemanticModel.PathComponent>) {
        val current = path.firstOrNull() ?: return
        when (current) {
            is SemanticModel.PathComponent.ParameterPathComponent -> appendLine("/:${current.parameter.parameter.name ?: "$"}")
            is SemanticModel.PathComponent.StringPathComponent -> appendLine("/${current.value}")
        }

        indent {
            path(path.drop(1))
        }
    }

    private fun print(init: StringBuilder.() -> Unit) {
        print(buildString(init))
    }

    private fun StringBuilder.separator() {
        appendLine("----------------")
    }

    private fun StringBuilder.separated(init: StringBuilder.() -> Unit) {
        separator()
        init()
        separator()
    }

    private fun StringBuilder.indent(indent: String = "    ", init: StringBuilder.() -> Unit) {
        val inner = buildString(init).takeIf { it.isNotBlank() } ?: return
        inner
            .lines()
            .dropLastWhile { it.isBlank() }
            .dropWhile { it.isBlank() }
            .forEach { line ->
                appendLine(indent + line)
            }
    }
}