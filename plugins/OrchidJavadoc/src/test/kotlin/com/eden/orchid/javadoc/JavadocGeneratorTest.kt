package com.eden.orchid.javadoc

import com.eden.orchid.testhelpers.OrchidIntegrationTest
import com.eden.orchid.testhelpers.pageWasRendered
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import strikt.api.expectThat


@DisplayName("Tests page-rendering behavior of Javadoc generator")
class JavadocGeneratorTest : OrchidIntegrationTest(JavadocModule()) {

    @Test
    @DisplayName("Java files are parsed, and pages are generated for each class and package.")
    fun test01() {
        configObject("javadoc", """{ "sourceDirs": "mockJava" }""")

        val testResults = execute()
        expectThat(testResults).pageWasRendered("/com/eden/orchid/mock/JavaClass1/index.html")
        expectThat(testResults).pageWasRendered("/com/eden/orchid/mock/JavaClass2/index.html")
        expectThat(testResults).pageWasRendered("/com/eden/orchid/mock/index.html")
    }

}