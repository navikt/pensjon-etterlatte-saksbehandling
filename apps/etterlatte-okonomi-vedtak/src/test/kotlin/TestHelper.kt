import java.io.FileNotFoundException

fun readFile(file: String) = VedtaksoversetterTest::class.java.getResource(file)?.readText()
    ?: throw FileNotFoundException("Fant ikke filen $file")
