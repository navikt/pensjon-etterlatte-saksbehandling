package no.nav.etterlatte.testsupport

import java.io.FileNotFoundException

object TestHelper

fun readFile(file: String) = TestHelper::class.java.getResource(file)?.readText()
    ?: throw FileNotFoundException("Fant ikke filen $file")