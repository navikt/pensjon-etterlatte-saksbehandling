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
    private val tillatteKoblingsfiler =
        setOf(
            "apps/etterlatte-behandling/src/main/kotlin/config/modules/BeregningRepositoryModule.kt",
            "apps/etterlatte-behandling/src/main/kotlin/config/modules/KlientModule.kt",
        )

    @Test
    fun `beregningsmodulen skal bare brukes i egen pakke og godkjente koblingsfiler`() {
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
                    val filsti = fil.relativeTo(prosjektrot).invariantSeparatorsPath
                    if (harEksternBeregningsreferanse(kildekode, filsti)) {
                        kildekode.lineSequence().mapIndexedNotNull { indeks, linje ->
                            if (beregningsreferanse.containsMatchIn(linje)) {
                                "$filsti:${indeks + 1}"
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
            "Beregningsmodulen skal bare brukes i $beregningspakke, underpakker og godkjente koblingsfiler.\n" +
                "Fjern referansene utenfor disse:\n${referanser.joinToString("\n")}"
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

    @ParameterizedTest
    @ValueSource(
        strings = [
            "apps/etterlatte-behandling/src/main/kotlin/config/modules/BeregningRepositoryModule.kt",
            "apps/etterlatte-behandling/src/main/kotlin/config/modules/KlientModule.kt",
        ],
    )
    fun `tillater referanser fra godkjente koblingsfiler`(filsti: String) {
        assertFalse(
            harEksternBeregningsreferanse(
                "package no.nav.etterlatte.config.modules\nimport no.nav.etterlatte.modules.beregning.BeregningRepository",
                filsti,
            ),
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "apps/etterlatte-behandling/src/main/kotlin/config/modules/ServiceModule.kt",
            "apps/etterlatte-behandling/src/main/kotlin/behandling/klienter/KlientModule.kt",
            "apps/etterlatte-beregning/src/main/kotlin/config/modules/KlientModule.kt",
        ],
    )
    fun `avviser referanser fra andre filer selv om mappen eller filnavnet ligner`(filsti: String) {
        assertTrue(
            harEksternBeregningsreferanse(
                "package no.nav.etterlatte.config.modules\nimport no.nav.etterlatte.modules.beregning.BeregningRepository",
                filsti,
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

    private fun harEksternBeregningsreferanse(
        kildekode: String,
        filsti: String = "",
    ): Boolean {
        val pakke = pakkedeklarasjon.find(kildekode)?.groupValues?.get(1)
        val tilhoererBeregningsmodulen = pakke == beregningspakke || pakke?.startsWith("$beregningspakke.") == true
        return filsti !in tillatteKoblingsfiler && !tilhoererBeregningsmodulen && beregningsreferanse.containsMatchIn(kildekode)
    }
}
