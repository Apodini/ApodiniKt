package de.tum.`in`.ase.apodini.internal

internal fun String.toCamelCase(): String {
    if (isEmpty()) return this
    if (toUpperCase() == this) return toLowerCase()

    val parts = parts
    return parts.first().toLowerCase() + parts.drop(1).joinToString("") { it.toLowerCase().capitalize() }
}

private val lowerToCapitalSplit = Regex("([a-z])([A-Z])")
private val uppercaseWordSplit = Regex("([A-Z]+)([A-Z][a-z]|\$)")
private val invalidCharacters = Regex("[^0-9a-zA-Z]")

private val String.parts: List<String>
    get() {
        val simpleSplits = replace(regex = lowerToCapitalSplit, replacement = "$1 $2")
        val splitAfterUppercaseWord = simpleSplits.replace(regex = uppercaseWordSplit, replacement = "$1 $2")
        return splitAfterUppercaseWord.split(regex = invalidCharacters)
    }
