package no.nav.etterlatte.pdl.mapper

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.pdl.PdlFolkeregistermetadata
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlMetadata
import no.nav.etterlatte.pdl.PdlVergeEllerFullmektig
import no.nav.etterlatte.pdl.PdlVergemaalEllerFremtidsfullmakt
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class VergeMapperTest {
    @Test
    fun `mapper opphoerstidspunkt for vergemaal hvis det er i framtiden`() {
        val enMaanedFraNaa = LocalDateTime.now().plusMonths(1)
        val pdlVergemaal =
            PdlVergemaalEllerFremtidsfullmakt(
                embete = null,
                folkeregistermetadata =
                    PdlFolkeregistermetadata(
                        opphoerstidspunkt = enMaanedFraNaa,
                    ),
                metadata =
                    PdlMetadata(
                        endringer = listOf(),
                        historisk = false,
                        master = "",
                        opplysningsId = "",
                    ),
                type = null,
                vergeEllerFullmektig =
                    PdlVergeEllerFullmektig(
                        motpartsPersonident = null,
                        navn = null,
                        tjenesteomraade = listOf(),
                        omfangetErInnenPersonligOmraade = null,
                    ),
            )

        val person =
            mockk<PdlHentPerson> {
                every { vergemaalEllerFremtidsfullmakt } returns listOf(pdlVergemaal)
            }
        val vergemaal = VergeMapper.mapVerge(person)
        vergemaal?.size shouldBe 1
        vergemaal?.get(0)?.opphoerstidspunkt shouldBe enMaanedFraNaa
    }

    @Test
    fun `filtrerer bort opphørte vergemål`() {
        val ettAarTilbakeITid = LocalDateTime.now().minusYears(1)
        val pdlVergeMaalOpphoert =
            PdlVergemaalEllerFremtidsfullmakt(
                embete = null,
                folkeregistermetadata =
                    PdlFolkeregistermetadata(
                        opphoerstidspunkt = ettAarTilbakeITid,
                    ),
                metadata =
                    PdlMetadata(
                        endringer = listOf(),
                        historisk = false,
                        master = "",
                        opplysningsId = "",
                    ),
                type = null,
                vergeEllerFullmektig =
                    PdlVergeEllerFullmektig(
                        motpartsPersonident = null,
                        navn = null,
                        tjenesteomraade = listOf(),
                        omfangetErInnenPersonligOmraade = null,
                    ),
            )

        val pdlvergemaalHistorisk =
            PdlVergemaalEllerFremtidsfullmakt(
                embete = null,
                folkeregistermetadata = null,
                metadata =
                    PdlMetadata(
                        endringer = listOf(),
                        historisk = true,
                        master = "",
                        opplysningsId = "",
                    ),
                type = null,
                vergeEllerFullmektig =
                    PdlVergeEllerFullmektig(
                        motpartsPersonident = null,
                        navn = null,
                        tjenesteomraade = listOf(),
                        omfangetErInnenPersonligOmraade = null,
                    ),
            )

        val pdlVergemaalGjeldende =
            PdlVergemaalEllerFremtidsfullmakt(
                embete = null,
                folkeregistermetadata = null,
                metadata =
                    PdlMetadata(
                        endringer = listOf(),
                        historisk = false,
                        master = "",
                        opplysningsId = "",
                    ),
                type = null,
                vergeEllerFullmektig =
                    PdlVergeEllerFullmektig(
                        motpartsPersonident = null,
                        navn = null,
                        tjenesteomraade = listOf(),
                        omfangetErInnenPersonligOmraade = null,
                    ),
            )

        val person =
            mockk<PdlHentPerson> {
                every { vergemaalEllerFremtidsfullmakt } returns listOf(pdlVergeMaalOpphoert, pdlVergemaalGjeldende, pdlvergemaalHistorisk)
            }

        val vergemaal = VergeMapper.mapVerge(person)
        vergemaal?.size shouldBe 1
        vergemaal?.get(0)?.opphoerstidspunkt shouldBe null
    }
}
