package no.nav.etterlatte.modules.beregning

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

internal class BeregningIsolasjonTest {
    private val beregningspakke = "no.nav.etterlatte.modules.beregning"
    private val pakkedeklarasjon = Regex("""(?m)^\s*package\s+([\w.]+)""")
    private val beregningsreferanse = Regex("""\b${Regex.escape(beregningspakke)}\b""")

    @Test
    fun `beregningsmodulen skal ikke brukes utenfor egen pakke`() {
        val prosjektrot =
            generateSequence(File(".").canonicalFile) { it.parentFile }
                .first { it.resolve("settings.gradle.kts").isFile }
        val referanser =
            sequenceOf("apps", "libs")
                .flatMap { mappe ->
                    requireNotNull(prosjektrot.resolve(mappe).listFiles()) {
                        "Finner ikke kildemappen $mappe i $prosjektrot"
                    }.asSequence()
                }.flatMap { it.resolve("src").walkTopDown() }
                .filter { it.isFile && it.extension in setOf("kt", "kts", "java") }
                .flatMap { fil ->
                    val kildekode = fil.readText()
                    if (harEksternBeregningsreferanse(kildekode)) {
                        kildekode.lineSequence().mapIndexedNotNull { indeks, linje ->
                            if (beregningsreferanse.containsMatchIn(linje)) {
                                "${fil.relativeTo(prosjektrot)}:${indeks + 1}"
                            } else {
                                null
                            }
                        }
                    } else {
                        emptySequence()
                    }
                }.sorted()
                .toList()

        assertTrue(referanser.isEmpty()) {
            "Beregningsmodulen er ikke aktivert og skal bare brukes i $beregningspakke og underpakker.\n" +
                "Fjern referansene utenfor modulen:\n${referanser.joinToString("\n")}"
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "import no.nav.etterlatte.modules.beregning.BeregningRepository",
            "import no.nav.etterlatte.modules.beregning.*",
            "import no.nav.etterlatte.modules.beregning.BeregningRepository as Repository",
            "val repository = no.nav.etterlatte.modules.beregning.BeregningRepository(dataSource)",
            "val maksTrygdetid = no.nav.etterlatte.modules.beregning.regler.MAKS_TRYGDETID",
        ],
    )
    fun `oppdager importer og fullt kvalifiserte referanser utenfra`(referanse: String) {
        assertTrue(harEksternBeregningsreferanse("package no.nav.etterlatte.config\n$referanse"))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "no.nav.etterlatte.modules.beregning",
            "no.nav.etterlatte.modules.beregning.avkorting",
            "no.nav.etterlatte.modules.beregning.grunnlag",
        ],
    )
    fun `tillater referanser innad i beregningsmodulen`(pakke: String) {
        assertFalse(
            harEksternBeregningsreferanse(
                "package $pakke\nimport no.nav.etterlatte.modules.beregning.BeregningRepository",
            ),
        )
    }

    @Test
    fun `forveksler ikke beregningsmodulen med pakker med lignende navn`() {
        assertFalse(
            harEksternBeregningsreferanse(
                "package no.nav.etterlatte.config\nimport no.nav.etterlatte.modules.beregningAnnet.Repository",
            ),
        )
    }

    private fun harEksternBeregningsreferanse(kildekode: String): Boolean {
        val pakke = pakkedeklarasjon.find(kildekode)?.groupValues?.get(1)
        val tilhoererBeregningsmodulen = pakke == beregningspakke || pakke?.startsWith("$beregningspakke.") == true
        return !tilhoererBeregningsmodulen && beregningsreferanse.containsMatchIn(kildekode)
    }
}
