package de.tum.`in`.ase.apodini.model

import de.tum.`in`.ase.apodini.test.TestWebService
import junit.framework.TestCase

internal class SemanticModelBuilderTest : TestCase() {

    fun testSemanticModel() {
        val semanticModel = TestWebService.semanticModel()
        print(semanticModel)
    }

}